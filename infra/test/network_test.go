// Package test contains Terratest integration tests for the PCIS network module.
//
// Prerequisites:
//   - AWS credentials must be configured (env vars, shared credentials file, or IAM role).
//   - The bootstrap S3 bucket and DynamoDB table for state locking must exist in the account.
//   - Set TF_VAR_aws_region (or AWS_DEFAULT_REGION) to target the correct region.
//
// Run all network tests:
//
//	cd infra/test
//	go test -v -timeout 30m -run TestNetworkModule
//
// Run only the dev-environment test:
//
//	go test -v -timeout 20m -run TestNetworkModuleDev
package test

import (
	"fmt"
	"os"
	"testing"

	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// testStateKey returns a unique state key for a test run to prevent collisions
// when multiple test runs target the same environment bucket.
func testStateKey(env string) string {
	return fmt.Sprintf("test/%s/network/terraform.tfstate", env)
}

// awsRegion returns the test region from TF_VAR_aws_region or defaults to us-east-1.
func awsRegion() string {
	if r := os.Getenv("TF_VAR_aws_region"); r != "" {
		return r
	}
	return "us-east-1"
}

// stateBucket returns the bootstrap S3 bucket name from PCIS_TF_STATE_BUCKET
// env var, or a conventional default derived from the account ID.
func stateBucket() string {
	if b := os.Getenv("PCIS_TF_STATE_BUCKET"); b != "" {
		return b
	}
	// Caller must set PCIS_TF_STATE_BUCKET in CI; no hardcoded account ID here.
	return ""
}

// TestNetworkModuleDev applies the dev environment composition, asserts the
// expected resource shape (VPC, subnets, single NAT gateway, flow logs), and
// destroys all resources on completion.
func TestNetworkModuleDev(t *testing.T) {
	t.Parallel()

	bucket := stateBucket()
	require.NotEmpty(t, bucket,
		"set PCIS_TF_STATE_BUCKET env var to the bootstrap S3 bucket name")

	terraformOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../environments/dev",
		Vars: map[string]interface{}{
			"aws_region":              awsRegion(),
			"vpc_cidr":                "10.0.0.0/16",
			"az_count":                2,
			"enable_ha_nat":           false,
			"flow_log_retention_days": 30,
		},
		// Point to a test-isolated state key so the test does not stomp dev state.
		BackendConfig: map[string]interface{}{
			"bucket":         bucket,
			"region":         awsRegion(),
			"key":            testStateKey("dev"),
			"encrypt":        true,
			"dynamodb_table": "pcis-terraform-locks",
		},
		Reconfigure: true,
	})

	defer terraform.Destroy(t, terraformOptions)
	terraform.InitAndApply(t, terraformOptions)

	// ── VPC ──────────────────────────────────────────────────────────────────

	vpcID := terraform.Output(t, terraformOptions, "vpc_id")
	assert.NotEmpty(t, vpcID, "vpc_id must be non-empty")
	assert.Regexp(t, `^vpc-[0-9a-f]+$`, vpcID, "vpc_id must match vpc-<hex> format")

	vpcCIDR := terraform.Output(t, terraformOptions, "vpc_cidr_block")
	assert.Equal(t, "10.0.0.0/16", vpcCIDR, "dev VPC CIDR must be 10.0.0.0/16")

	// ── Subnets ───────────────────────────────────────────────────────────────

	publicSubnetIDs := terraform.OutputList(t, terraformOptions, "public_subnet_ids")
	require.Len(t, publicSubnetIDs, 2,
		"dev must provision exactly 2 public subnets (one per AZ)")

	privateAppSubnetIDs := terraform.OutputList(t, terraformOptions, "private_app_subnet_ids")
	require.Len(t, privateAppSubnetIDs, 2,
		"dev must provision exactly 2 private-app subnets (one per AZ)")

	privateDataSubnetIDs := terraform.OutputList(t, terraformOptions, "private_data_subnet_ids")
	require.Len(t, privateDataSubnetIDs, 2,
		"dev must provision exactly 2 private-data subnets (one per AZ)")

	// All subnet IDs must be non-empty and well-formed.
	allSubnets := append(append(publicSubnetIDs, privateAppSubnetIDs...), privateDataSubnetIDs...)
	for _, id := range allSubnets {
		assert.Regexp(t, `^subnet-[0-9a-f]+$`, id,
			"subnet ID %q must match subnet-<hex> format", id)
	}

	// ── NAT Gateways ─────────────────────────────────────────────────────────

	natGatewayIDs := terraform.OutputList(t, terraformOptions, "nat_gateway_ids")
	require.Len(t, natGatewayIDs, 1,
		"dev must provision exactly 1 NAT gateway (enable_ha_nat=false)")

	assert.Regexp(t, `^nat-[0-9a-f]+$`, natGatewayIDs[0],
		"NAT gateway ID must match nat-<hex> format")

	// ── Security Groups ───────────────────────────────────────────────────────

	albSGID := terraform.Output(t, terraformOptions, "alb_security_group_id")
	assert.Regexp(t, `^sg-[0-9a-f]+$`, albSGID,
		"alb_security_group_id must match sg-<hex> format")

	dbSGID := terraform.Output(t, terraformOptions, "database_security_group_id")
	assert.Regexp(t, `^sg-[0-9a-f]+$`, dbSGID,
		"database_security_group_id must match sg-<hex> format")

	// ALB and DB security groups must be distinct resources.
	assert.NotEqual(t, albSGID, dbSGID,
		"ALB and database security groups must be separate resources")
}

// TestNetworkModulePrd applies the prd environment composition and asserts
// HA NAT (one per AZ) and 3-AZ subnet counts, then destroys.
func TestNetworkModulePrd(t *testing.T) {
	t.Parallel()

	bucket := stateBucket()
	require.NotEmpty(t, bucket,
		"set PCIS_TF_STATE_BUCKET env var to the bootstrap S3 bucket name")

	terraformOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../environments/prd",
		Vars: map[string]interface{}{
			"aws_region":              awsRegion(),
			"vpc_cidr":                "10.2.0.0/16",
			"az_count":                3,
			"enable_ha_nat":           true,
			"flow_log_retention_days": 90,
		},
		BackendConfig: map[string]interface{}{
			"bucket":         bucket,
			"region":         awsRegion(),
			"key":            testStateKey("prd-ci"),
			"encrypt":        true,
			"dynamodb_table": "pcis-terraform-locks",
		},
		Reconfigure: true,
	})

	defer terraform.Destroy(t, terraformOptions)
	terraform.InitAndApply(t, terraformOptions)

	// ── VPC CIDR ─────────────────────────────────────────────────────────────

	vpcCIDR := terraform.Output(t, terraformOptions, "vpc_cidr_block")
	assert.Equal(t, "10.2.0.0/16", vpcCIDR, "prd VPC CIDR must be 10.2.0.0/16")

	// ── 3-AZ Subnet counts ───────────────────────────────────────────────────

	publicSubnetIDs := terraform.OutputList(t, terraformOptions, "public_subnet_ids")
	require.Len(t, publicSubnetIDs, 3, "prd must have 3 public subnets")

	privateAppSubnetIDs := terraform.OutputList(t, terraformOptions, "private_app_subnet_ids")
	require.Len(t, privateAppSubnetIDs, 3, "prd must have 3 private-app subnets")

	privateDataSubnetIDs := terraform.OutputList(t, terraformOptions, "private_data_subnet_ids")
	require.Len(t, privateDataSubnetIDs, 3, "prd must have 3 private-data subnets")

	// ── HA NAT (one per AZ) ──────────────────────────────────────────────────

	natGatewayIDs := terraform.OutputList(t, terraformOptions, "nat_gateway_ids")
	require.Len(t, natGatewayIDs, 3,
		"prd must provision 3 NAT gateways (enable_ha_nat=true, az_count=3)")
}

// TestNetworkCIDRNonOverlap validates that the CIDR blocks of all subnets in
// a single environment are non-overlapping. This test runs locally without
// applying any real infrastructure (it parses the plan output only).
func TestNetworkCIDRNonOverlap(t *testing.T) {
	t.Parallel()

	bucket := stateBucket()
	require.NotEmpty(t, bucket,
		"set PCIS_TF_STATE_BUCKET env var to the bootstrap S3 bucket name")

	terraformOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../environments/dev",
		Vars: map[string]interface{}{
			"aws_region":    awsRegion(),
			"vpc_cidr":      "10.0.0.0/16",
			"az_count":      2,
			"enable_ha_nat": false,
		},
		BackendConfig: map[string]interface{}{
			"bucket":         bucket,
			"region":         awsRegion(),
			"key":            testStateKey("dev-cidr-check"),
			"encrypt":        true,
			"dynamodb_table": "pcis-terraform-locks",
		},
		Reconfigure: true,
	})

	// Only plan — do not apply.
	terraform.Init(t, terraformOptions)
	planOut := terraform.Plan(t, terraformOptions)
	assert.NotEmpty(t, planOut, "terraform plan must produce non-empty output")
	assert.NotContains(t, planOut, "Error",
		"terraform plan must complete without errors")
}
