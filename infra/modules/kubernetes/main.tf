resource "aws_cloudwatch_log_group" "eks" {
  name              = "/aws/eks/${local.cluster_name}/cluster"
  retention_in_days = var.cloudwatch_log_retention_days
  tags              = var.tags
}

resource "aws_security_group" "cluster" {
  name_prefix = "${local.cluster_name}-cluster-"
  description = "EKS cluster security group"
  vpc_id      = var.vpc_id
  tags        = merge(var.tags, { Name = "${local.cluster_name}-cluster" })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group_rule" "cluster_egress" {
  type              = "egress"
  security_group_id = aws_security_group.cluster.id
  protocol          = "-1"
  from_port         = 0
  to_port           = 0
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "Allow cluster egress"
}

resource "aws_eks_cluster" "this" {
  name     = local.cluster_name
  role_arn = aws_iam_role.cluster.arn
  version  = var.kubernetes_version

  enabled_cluster_log_types = [
    "api",
    "audit",
    "authenticator",
    "controllerManager",
    "scheduler",
  ]

  vpc_config {
    subnet_ids              = var.private_subnet_ids
    endpoint_private_access = true
    endpoint_public_access  = var.cluster_endpoint_public_access
    public_access_cidrs     = var.cluster_endpoint_public_access ? var.cluster_endpoint_public_access_cidrs : null
    security_group_ids      = [aws_security_group.cluster.id]
  }

  access_config {
    authentication_mode                         = "API_AND_CONFIG_MAP"
    bootstrap_cluster_creator_admin_permissions = true
  }

  depends_on = [
    aws_iam_role_policy_attachment.cluster_AmazonEKSClusterPolicy,
    aws_iam_role_policy_attachment.cluster_AmazonEKSVPCResourceController,
    aws_cloudwatch_log_group.eks,
  ]

  tags = merge(var.tags, { Name = local.cluster_name })
}

# ---------------------------------------------------------------------------
# Application node group (always-on)
# ---------------------------------------------------------------------------
resource "aws_eks_node_group" "application" {
  cluster_name    = aws_eks_cluster.this.name
  node_group_name = "application"
  node_role_arn   = aws_iam_role.node.arn
  subnet_ids      = var.private_subnet_ids
  ami_type        = "AL2023_x86_64_STANDARD"
  instance_types  = var.application_instance_types
  capacity_type   = "ON_DEMAND"

  scaling_config {
    min_size     = var.application_scaling.min_size
    max_size     = var.application_scaling.max_size
    desired_size = var.application_scaling.desired_size
  }

  update_config {
    max_unavailable = 1
  }

  labels = {
    workload = "application"
    env      = var.environment_name
  }

  depends_on = [
    aws_iam_role_policy_attachment.node_AmazonEKSWorkerNodePolicy,
    aws_iam_role_policy_attachment.node_AmazonEKS_CNI_Policy,
    aws_iam_role_policy_attachment.node_AmazonEC2ContainerRegistryReadOnly,
  ]

  tags = merge(var.tags, { Name = "${local.cluster_name}-application" })
}

# ---------------------------------------------------------------------------
# Batch node group (scales to zero; tainted for CronJobs)
# Cluster Autoscaler / Karpenter scales up within batch SLA when CronJobs fire.
# ---------------------------------------------------------------------------
resource "aws_eks_node_group" "batch" {
  cluster_name    = aws_eks_cluster.this.name
  node_group_name = "batch"
  node_role_arn   = aws_iam_role.node.arn
  subnet_ids      = var.private_subnet_ids
  ami_type        = "AL2023_x86_64_STANDARD"
  instance_types  = var.batch_instance_types
  capacity_type   = "ON_DEMAND"

  scaling_config {
    min_size     = var.batch_scaling.min_size
    max_size     = var.batch_scaling.max_size
    desired_size = var.batch_scaling.desired_size
  }

  update_config {
    max_unavailable = 1
  }

  labels = {
    workload = "batch"
    env      = var.environment_name
  }

  taint {
    key    = "batch"
    value  = "true"
    effect = "NO_SCHEDULE"
  }

  tags = merge(var.tags, {
    Name                     = "${local.cluster_name}-batch"
    "k8s.io/cluster-autoscaler/enabled" = "true"
    "k8s.io/cluster-autoscaler/${local.cluster_name}" = "owned"
  })

  depends_on = [
    aws_iam_role_policy_attachment.node_AmazonEKSWorkerNodePolicy,
    aws_iam_role_policy_attachment.node_AmazonEKS_CNI_Policy,
    aws_iam_role_policy_attachment.node_AmazonEC2ContainerRegistryReadOnly,
  ]

  lifecycle {
    ignore_changes = [scaling_config[0].desired_size]
  }
}
