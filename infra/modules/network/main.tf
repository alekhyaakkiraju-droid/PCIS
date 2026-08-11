data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  azs = slice(data.aws_availability_zones.available.names, 0, min(var.az_count, length(data.aws_availability_zones.available.names)))

  # Non-overlapping /20 slices of the VPC CIDR for az_count 2 or 3.
  # Layout (example 10.0.0.0/16, 2 AZs):
  #   public 0-1, private-app 2-3, private-data 4-5.
  # Private-app uses /20 per AZ (equivalent aggregate capacity to a /19 across 2 AZs).
  public_subnet_cidrs = [
    for i, az in local.azs : cidrsubnet(var.vpc_cidr, 4, i)
  ]
  az_len = length(local.azs)

  private_app_subnet_cidrs = [
    for i, az in local.azs : cidrsubnet(var.vpc_cidr, 4, i + local.az_len)
  ]
  private_data_subnet_cidrs = [
    for i, az in local.azs : cidrsubnet(var.vpc_cidr, 4, i + (2 * local.az_len))
  ]

  base_tags = merge(
    {
      Project   = "PCIS"
      ManagedBy = "Terraform"
    },
    var.tags,
    {
      Environment = var.environment_name
    }
  )

  nat_gateway_count = var.enable_ha_nat ? length(local.azs) : 1
}

resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-vpc"
  })
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-igw"
  })
}

resource "aws_subnet" "public" {
  count = length(local.azs)

  vpc_id                  = aws_vpc.this.id
  cidr_block              = local.public_subnet_cidrs[count.index]
  availability_zone       = local.azs[count.index]
  map_public_ip_on_launch = true

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-public-${local.azs[count.index]}"
    Tier = "public"
    Zone = "Public"
  })
}

resource "aws_subnet" "private_app" {
  count = length(local.azs)

  vpc_id            = aws_vpc.this.id
  cidr_block        = local.private_app_subnet_cidrs[count.index]
  availability_zone = local.azs[count.index]

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-private-app-${local.azs[count.index]}"
    Tier = "private-app"
    Zone = "Internal"
  })
}

resource "aws_subnet" "private_data" {
  count = length(local.azs)

  vpc_id            = aws_vpc.this.id
  cidr_block        = local.private_data_subnet_cidrs[count.index]
  availability_zone = local.azs[count.index]

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-private-data-${local.azs[count.index]}"
    Tier = "private-data"
    Zone = "Data"
  })
}

resource "aws_eip" "nat" {
  count  = local.nat_gateway_count
  domain = "vpc"

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-nat-eip-${count.index}"
  })

  depends_on = [aws_internet_gateway.this]
}

resource "aws_nat_gateway" "this" {
  count = local.nat_gateway_count

  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-nat-${count.index}"
  })

  depends_on = [aws_internet_gateway.this]
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-public-rt"
  })
}

resource "aws_route" "public_internet" {
  route_table_id         = aws_route_table.public.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.this.id
}

resource "aws_route_table_association" "public" {
  count = length(aws_subnet.public)

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table" "private_app" {
  count = length(local.azs)

  vpc_id = aws_vpc.this.id

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-private-app-rt-${local.azs[count.index]}"
  })
}

resource "aws_route" "private_app_nat" {
  count = length(local.azs)

  route_table_id         = aws_route_table.private_app[count.index].id
  destination_cidr_block = "0.0.0.0/0"
  # Single-NAT mode still routes ALL private subnets through NAT in AZ index 0.
  nat_gateway_id = aws_nat_gateway.this[var.enable_ha_nat ? count.index : 0].id
}

resource "aws_route_table_association" "private_app" {
  count = length(aws_subnet.private_app)

  subnet_id      = aws_subnet.private_app[count.index].id
  route_table_id = aws_route_table.private_app[count.index].id
}

resource "aws_route_table" "private_data" {
  count = length(local.azs)

  vpc_id = aws_vpc.this.id

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-private-data-rt-${local.azs[count.index]}"
  })
}

resource "aws_route" "private_data_nat" {
  count = length(local.azs)

  route_table_id         = aws_route_table.private_data[count.index].id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.this[var.enable_ha_nat ? count.index : 0].id
}

resource "aws_route_table_association" "private_data" {
  count = length(aws_subnet.private_data)

  subnet_id      = aws_subnet.private_data[count.index].id
  route_table_id = aws_route_table.private_data[count.index].id
}

# ---------------------------------------------------------------------------
# VPC Flow Logs → S3 (default retention 90 days)
# ---------------------------------------------------------------------------

resource "aws_s3_bucket" "flow_logs" {
  bucket_prefix = "pcis-${var.environment_name}-vpc-flow-logs-"

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-vpc-flow-logs"
  })
}

resource "aws_s3_bucket_public_access_block" "flow_logs" {
  bucket = aws_s3_bucket.flow_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "flow_logs" {
  bucket = aws_s3_bucket.flow_logs.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "flow_logs" {
  bucket = aws_s3_bucket.flow_logs.id

  rule {
    id     = "expire-flow-logs"
    status = "Enabled"

    filter {}

    expiration {
      days = var.flow_log_retention_days
    }
  }
}

resource "aws_flow_log" "this" {
  log_destination      = aws_s3_bucket.flow_logs.arn
  log_destination_type = "s3"
  traffic_type         = "ALL"
  vpc_id               = aws_vpc.this.id

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-vpc-flow-log"
  })
}

# ---------------------------------------------------------------------------
# Security groups — default deny inbound; explicit allow for ALB / mesh / DB
# ---------------------------------------------------------------------------

resource "aws_default_security_group" "default" {
  vpc_id = aws_vpc.this.id

  # Explicitly omit ingress rules → deny all inbound.
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-default-sg"
  })
}

resource "aws_security_group" "alb" {
  name_prefix = "pcis-${var.environment_name}-alb-"
  description = "ALB ingress (HTTPS 443) — Public zone"
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "HTTPS from internet"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-alb-sg"
    Zone = "Public"
  })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group" "mesh" {
  name_prefix = "pcis-${var.environment_name}-mesh-"
  description = "Inter-service mesh traffic — Internal zone"
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "Mesh mTLS / service traffic within VPC"
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    self        = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-mesh-sg"
    Zone = "Internal"
  })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group" "database" {
  name_prefix = "pcis-${var.environment_name}-db-"
  description = "Database access from application subnets only — Data zone"
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "PostgreSQL from private application subnets"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = local.private_app_subnet_cidrs
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-db-sg"
    Zone = "Data"
  })

  lifecycle {
    create_before_destroy = true
  }
}
