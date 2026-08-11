# ─────────────────────────────────────────────────────────────────────────────
# Data Sources
# ─────────────────────────────────────────────────────────────────────────────

data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}

# ─────────────────────────────────────────────────────────────────────────────
# Locals
# ─────────────────────────────────────────────────────────────────────────────

locals {
  # Slice the region's AZ list to the requested count, sorted alphabetically for
  # deterministic subnet placement even if AWS adds new AZs to the region.
  azs = slice(sort(data.aws_availability_zones.available.names), 0, var.az_count)

  # Subnet CIDR layout from a /16 VPC:
  #
  #   Public   /20  → indices  0 ..  2  (10.x.  0.0/20, 10.x. 16.0/20, 10.x. 32.0/20)
  #   App      /19  → indices  3 ..  5  (10.x. 96.0/19, 10.x.128.0/19, 10.x.160.0/19)
  #   Data     /20  → indices 12 .. 14  (10.x.192.0/20, 10.x.208.0/20, 10.x.224.0/20)
  #
  # There is no CIDR overlap between tiers; the gaps between ranges are
  # available for future subnet tiers (e.g. transit/management).
  public_subnet_cidrs      = [for i in range(var.az_count) : cidrsubnet(var.vpc_cidr, 4, i)]
  private_app_subnet_cidrs = [for i in range(var.az_count) : cidrsubnet(var.vpc_cidr, 3, i + 3)]
  private_data_subnet_cidrs = [for i in range(var.az_count) : cidrsubnet(var.vpc_cidr, 4, i + 12)]

  # All resources receive these three tags plus any caller-supplied common_tags.
  base_tags = merge(var.common_tags, {
    Environment = var.environment_name
    Project     = "PCIS"
    ManagedBy   = "Terraform"
  })
}

# ─────────────────────────────────────────────────────────────────────────────
# VPC
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-vpc"
  })
}

# ─────────────────────────────────────────────────────────────────────────────
# Internet Gateway (attached to public subnets only)
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-igw"
  })
}

# ─────────────────────────────────────────────────────────────────────────────
# Public Subnets — Security Zone: Public (ALB / WAF)
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_subnet" "public" {
  count = var.az_count

  vpc_id                  = aws_vpc.main.id
  cidr_block              = local.public_subnet_cidrs[count.index]
  availability_zone       = local.azs[count.index]
  map_public_ip_on_launch = true

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-public-${local.azs[count.index]}"
    Tier = "Public"
    "kubernetes.io/role/elb" = "1"
  })
}

# ─────────────────────────────────────────────────────────────────────────────
# Private Application Subnets — Security Zones: DMZ + Internal (API GW / Service Mesh)
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_subnet" "private_app" {
  count = var.az_count

  vpc_id            = aws_vpc.main.id
  cidr_block        = local.private_app_subnet_cidrs[count.index]
  availability_zone = local.azs[count.index]

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-private-app-${local.azs[count.index]}"
    Tier = "PrivateApp"
    "kubernetes.io/role/internal-elb" = "1"
  })
}

# ─────────────────────────────────────────────────────────────────────────────
# Private Data Subnets — Security Zone: Data (Aurora, ElastiCache, MSK)
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_subnet" "private_data" {
  count = var.az_count

  vpc_id            = aws_vpc.main.id
  cidr_block        = local.private_data_subnet_cidrs[count.index]
  availability_zone = local.azs[count.index]

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-private-data-${local.azs[count.index]}"
    Tier = "PrivateData"
  })
}

# ─────────────────────────────────────────────────────────────────────────────
# Elastic IPs for NAT Gateways
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_eip" "nat" {
  # HA mode  → one EIP per AZ
  # Non-HA   → single EIP
  count  = var.enable_ha_nat ? var.az_count : 1
  domain = "vpc"

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-nat-eip-${count.index + 1}"
  })

  depends_on = [aws_internet_gateway.main]
}

# ─────────────────────────────────────────────────────────────────────────────
# NAT Gateways
#
# HA (prd)   → one per AZ; each private route table routes to its local NAT.
# Single     → placed in public subnet of AZ-1; ALL private subnets (all AZs)
#              route their egress through this single NAT gateway.  This
#              satisfies the edge-case requirement that single-AZ mode must
#              still provide egress for private subnets in every AZ.
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_nat_gateway" "main" {
  count = var.enable_ha_nat ? var.az_count : 1

  allocation_id = aws_eip.nat[count.index].id
  # Always placed in the public subnet of the corresponding AZ (or AZ-1 for single).
  subnet_id     = aws_subnet.public[count.index].id

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-nat-${count.index + 1}"
  })

  depends_on = [aws_internet_gateway.main]
}

# ─────────────────────────────────────────────────────────────────────────────
# Route Tables — Public
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-rt-public"
  })
}

resource "aws_route_table_association" "public" {
  count = var.az_count

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# ─────────────────────────────────────────────────────────────────────────────
# Route Tables — Private
#
# HA   → one route table per AZ pointing to the co-located NAT gateway.
# Single → one shared route table; all private subnets reference it.
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_route_table" "private" {
  count  = var.enable_ha_nat ? var.az_count : 1
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main[count.index].id
  }

  tags = merge(local.base_tags, {
    Name = (
      var.enable_ha_nat
      ? "pcis-${var.environment_name}-rt-private-${local.azs[count.index]}"
      : "pcis-${var.environment_name}-rt-private"
    )
  })
}

resource "aws_route_table_association" "private_app" {
  count = var.az_count

  subnet_id      = aws_subnet.private_app[count.index].id
  # HA: each AZ routes to its own NAT; non-HA: all route through route table index 0.
  route_table_id = aws_route_table.private[var.enable_ha_nat ? count.index : 0].id
}

resource "aws_route_table_association" "private_data" {
  count = var.az_count

  subnet_id      = aws_subnet.private_data[count.index].id
  route_table_id = aws_route_table.private[var.enable_ha_nat ? count.index : 0].id
}

# ─────────────────────────────────────────────────────────────────────────────
# Default Security Group — deny all inbound (default-deny posture)
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_default_security_group" "main" {
  vpc_id = aws_vpc.main.id

  # No ingress rules  → deny all inbound traffic.
  # No egress rules   → deny all outbound traffic.
  # Workloads must use explicitly scoped security groups below.

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-default-sg-deny-all"
  })
}

# ─────────────────────────────────────────────────────────────────────────────
# Security Group — ALB (Public zone ingress: 443 + 80 redirect)
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_security_group" "alb" {
  name        = "pcis-${var.environment_name}-alb-sg"
  description = "ALB: HTTPS ingress from internet; traffic exits to private-app subnets"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTPS from internet"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP redirect from internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "All outbound to private-app subnets"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = local.private_app_subnet_cidrs
  }

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-alb-sg"
  })
}

# ─────────────────────────────────────────────────────────────────────────────
# Security Group — Service Mesh (Internal zone: mTLS inter-service traffic)
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_security_group" "service_mesh" {
  name        = "pcis-${var.environment_name}-service-mesh-sg"
  description = "Service mesh: mTLS (Istio Envoy) + application traffic within private-app subnets"
  vpc_id      = aws_vpc.main.id

  # Istio Envoy proxy control-plane and data-plane ports (self-referencing for mesh peers)
  ingress {
    description = "Istio Envoy proxy ports (mTLS inter-service)"
    from_port   = 15000
    to_port     = 15090
    protocol    = "tcp"
    self        = true
  }

  # Application traffic from private-app tier (all AZs)
  ingress {
    description = "Application HTTP traffic from private-app subnets"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = local.private_app_subnet_cidrs
  }

  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-service-mesh-sg"
  })
}

# ─────────────────────────────────────────────────────────────────────────────
# Security Group — Database (Data zone: access from app subnets only)
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_security_group" "database" {
  name        = "pcis-${var.environment_name}-database-sg"
  description = "Aurora/ElastiCache/MSK: inbound only from private-app subnets"
  vpc_id      = aws_vpc.main.id

  # PostgreSQL — Aurora
  ingress {
    description = "PostgreSQL (Aurora) from private-app subnets only"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = local.private_app_subnet_cidrs
  }

  # Redis — ElastiCache
  ingress {
    description = "Redis (ElastiCache) from private-app subnets only"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = local.private_app_subnet_cidrs
  }

  # Kafka — MSK (plaintext and TLS broker ports)
  ingress {
    description = "Kafka (MSK) from private-app subnets only"
    from_port   = 9092
    to_port     = 9096
    protocol    = "tcp"
    cidr_blocks = local.private_app_subnet_cidrs
  }

  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-database-sg"
  })
}

# ─────────────────────────────────────────────────────────────────────────────
# VPC Flow Logs — delivered to S3 with lifecycle policy
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_s3_bucket" "flow_logs" {
  # Bucket name includes the AWS account ID to guarantee global uniqueness
  # without hardcoding it (resolved via data source).
  bucket = "pcis-${var.environment_name}-vpc-flow-logs-${data.aws_caller_identity.current.account_id}"

  tags = merge(local.base_tags, {
    Name    = "pcis-${var.environment_name}-vpc-flow-logs"
    Purpose = "VPCFlowLogs"
  })
}

resource "aws_s3_bucket_server_side_encryption_configuration" "flow_logs" {
  bucket = aws_s3_bucket.flow_logs.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "flow_logs" {
  bucket = aws_s3_bucket.flow_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "flow_logs" {
  bucket = aws_s3_bucket.flow_logs.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "flow_logs" {
  # Versioning must be enabled before lifecycle rules are applied.
  depends_on = [aws_s3_bucket_versioning.flow_logs]

  bucket = aws_s3_bucket.flow_logs.id

  rule {
    id     = "expire-flow-log-objects"
    status = "Enabled"

    filter {}

    expiration {
      days = var.flow_log_retention_days
    }

    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }
}

resource "aws_s3_bucket_policy" "flow_logs" {
  bucket = aws_s3_bucket.flow_logs.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AWSLogDeliveryWrite"
        Effect = "Allow"
        Principal = {
          Service = "delivery.logs.amazonaws.com"
        }
        Action   = "s3:PutObject"
        Resource = "${aws_s3_bucket.flow_logs.arn}/AWSLogs/${data.aws_caller_identity.current.account_id}/*"
        Condition = {
          StringEquals = {
            "s3:x-amz-acl"          = "bucket-owner-full-control"
            "aws:SourceAccount"      = data.aws_caller_identity.current.account_id
          }
        }
      },
      {
        Sid    = "AWSLogDeliveryAclCheck"
        Effect = "Allow"
        Principal = {
          Service = "delivery.logs.amazonaws.com"
        }
        Action   = "s3:GetBucketAcl"
        Resource = aws_s3_bucket.flow_logs.arn
        Condition = {
          StringEquals = {
            "aws:SourceAccount" = data.aws_caller_identity.current.account_id
          }
        }
      }
    ]
  })
}

resource "aws_flow_log" "main" {
  vpc_id = aws_vpc.main.id

  traffic_type         = "ALL"
  log_destination_type = "s3"
  log_destination      = "${aws_s3_bucket.flow_logs.arn}/"

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-vpc-flow-log"
  })

  depends_on = [aws_s3_bucket_policy.flow_logs]
}
