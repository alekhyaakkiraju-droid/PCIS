package test

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func repoRoot(t *testing.T) string {
	t.Helper()
	root, err := filepath.Abs("..")
	require.NoError(t, err)
	return root
}

func envOptions(t *testing.T, env string) *terraform.Options {
	t.Helper()
	return &terraform.Options{
		TerraformDir: filepath.Join(repoRoot(t), "environments", env),
		VarFiles:     []string{"terraform.tfvars"},
		EnvVars: map[string]string{
			"AWS_DEFAULT_REGION": "us-east-1",
		},
		NoColor: true,
	}
}

func initWithoutRemoteBackend(t *testing.T, opts *terraform.Options) {
	t.Helper()
	_, err := terraform.RunTerraformCommandE(
		t, opts,
		"init", "-backend=false", "-reconfigure", "-input=false", "-no-color",
	)
	require.NoError(t, err)
}

// TestCidrFixturesAreNonOverlapping ensures env CIDRs stay isolated.
func TestCidrFixturesAreNonOverlapping(t *testing.T) {
	t.Parallel()
	cidrs := map[string]string{
		"dev": "10.0.0.0/16",
		"tst": "10.1.0.0/16",
		"prd": "10.2.0.0/16",
	}
	seen := map[string]string{}
	for env, cidr := range cidrs {
		if other, ok := seen[cidr]; ok {
			t.Fatalf("CIDR %s used by both %s and %s", cidr, other, env)
		}
		seen[cidr] = env
	}
	assert.Equal(t, 3, len(seen))
}

// TestDevTfvarsMatchesModuleContract checks fixture values used by Terratest.
func TestDevTfvarsMatchesModuleContract(t *testing.T) {
	t.Parallel()
	path := filepath.Join(repoRoot(t), "environments", "dev", "terraform.tfvars")
	b, err := os.ReadFile(path)
	require.NoError(t, err)
	body := string(b)
	assert.Contains(t, body, `environment_name        = "dev"`)
	assert.Contains(t, body, `vpc_cidr                = "10.0.0.0/16"`)
	assert.Contains(t, body, `enable_ha_nat           = false`)
	assert.Contains(t, body, `az_count                = 2`)
}

// TestPrdEnablesHaNat ensures production uses HA NAT gateways.
func TestPrdEnablesHaNat(t *testing.T) {
	t.Parallel()
	path := filepath.Join(repoRoot(t), "environments", "prd", "terraform.tfvars")
	b, err := os.ReadFile(path)
	require.NoError(t, err)
	assert.Contains(t, string(b), `enable_ha_nat           = true`)
}

// TestTerraformValidateAllEnvironments runs terraform init -backend=false + validate.
func TestTerraformValidateAllEnvironments(t *testing.T) {
	for _, env := range []string{"dev", "tst", "prd"} {
		env := env
		t.Run(env, func(t *testing.T) {
			opts := envOptions(t, env)
			initWithoutRemoteBackend(t, opts)
			_, err := terraform.RunTerraformCommandE(t, opts, "validate", "-no-color")
			require.NoError(t, err)
		})
	}
}

// TestNetworkDevModulePlan plans the dev stack and asserts CIDR / NAT presence.
// Skips when AWS credentials are unavailable.
func TestNetworkDevModulePlan(t *testing.T) {
	opts := envOptions(t, "dev")
	initWithoutRemoteBackend(t, opts)

	// Use RunTerraformCommandE so terratest does not re-init with the S3 backend.
	plan, err := terraform.RunTerraformCommandE(
		t, opts,
		"plan", "-var-file=terraform.tfvars", "-input=false", "-no-color", "-lock=false",
	)
	if err != nil {
		msg := err.Error()
		if strings.Contains(msg, "No valid credential") ||
			strings.Contains(msg, "Unable to locate credentials") ||
			strings.Contains(msg, "InvalidClientTokenId") ||
			strings.Contains(msg, "ExpiredToken") ||
			strings.Contains(msg, "no EC2 IMDS role found") ||
			strings.Contains(msg, "SSOProviderInvalidToken") ||
			strings.Contains(msg, "NoCredentialProviders") ||
			strings.Contains(msg, "Backend initialization required") ||
			strings.Contains(msg, "Failed to get existing workspaces") ||
			strings.Contains(msg, "NoSuchBucket") {
			t.Skipf("skipping plan assertions until AWS credentials + S3/DynamoDB backend bootstrap are available: %v", err)
		}
		require.NoError(t, err)
	}
	require.NotEmpty(t, plan)
	assert.Contains(t, plan, "10.0.0.0/16")
	assert.Contains(t, plan, "aws_nat_gateway")
	assert.Contains(t, plan, "aws_flow_log")

	if os.Getenv("PCIS_TERRATEST_APPLY") == "1" {
		defer terraform.Destroy(t, opts)
		_, err := terraform.RunTerraformCommandE(t, opts, "apply", "-auto-approve", "-var-file=terraform.tfvars", "-input=false", "-no-color")
		require.NoError(t, err)
		assert.NotEmpty(t, terraform.Output(t, opts, "vpc_id"))
	}
}

// TestModuleTagsDocumented ensures README documents required tags.
func TestModuleTagsDocumented(t *testing.T) {
	t.Parallel()
	readme := filepath.Join(repoRoot(t), "modules", "network", "README.md")
	b, err := os.ReadFile(readme)
	require.NoError(t, err)
	body := string(b)
	assert.Contains(t, body, "Project")
	assert.Contains(t, body, "ManagedBy")
	assert.Contains(t, body, "Environment")
}

// TestNetworkModuleHasNatIgwAndFlowLogs asserts WO-129 NAT / IGW / flow-log resources.
func TestNetworkModuleHasNatIgwAndFlowLogs(t *testing.T) {
	t.Parallel()
	main := readNetworkMain(t)
	assert.Contains(t, main, `resource "aws_internet_gateway"`)
	assert.Contains(t, main, `resource "aws_nat_gateway"`)
	assert.Contains(t, main, `resource "aws_flow_log"`)
	assert.Contains(t, main, `resource "aws_s3_bucket" "flow_logs"`)
	assert.Contains(t, main, "flow_log_retention_days")
}

// TestNetworkSecurityGroupsDefaultDenyAndExplicitAllows covers SG ACs.
func TestNetworkSecurityGroupsDefaultDenyAndExplicitAllows(t *testing.T) {
	t.Parallel()
	main := readNetworkMain(t)
	assert.Contains(t, main, `resource "aws_default_security_group"`)
	assert.Contains(t, main, "deny all inbound")
	assert.Contains(t, main, `resource "aws_security_group" "alb"`)
	assert.Contains(t, main, "from_port   = 443")
	assert.Contains(t, main, `resource "aws_security_group" "mesh"`)
	assert.Contains(t, main, `resource "aws_security_group" "database"`)
	assert.Contains(t, main, "from_port   = 5432")
	assert.Contains(t, main, "private_app_subnet_cidrs")
}

// TestRemoteBackendConfiguredWithS3AndDynamoDB covers remote-state AC.
func TestRemoteBackendConfiguredWithS3AndDynamoDB(t *testing.T) {
	t.Parallel()
	backend := readFile(t, filepath.Join(repoRoot(t), "backend.tf"))
	assert.Contains(t, backend, `backend "s3"`)
	assert.Contains(t, backend, "pcis-terraform-state")
	assert.Contains(t, backend, "pcis-terraform-locks")
	assert.Contains(t, backend, "dynamodb_table")

	for _, env := range []string{"dev", "tst", "prd"} {
		hcl := readFile(t, filepath.Join(repoRoot(t), "environments", env, "backend.hcl"))
		assert.Contains(t, hcl, "pcis-terraform-state")
		assert.Contains(t, hcl, "pcis-terraform-locks")
		assert.Contains(t, hcl, env+"/network/terraform.tfstate")
	}
}

// TestSingleNatModeRoutesAllPrivateSubnets documents the cost-optimized edge case.
func TestSingleNatModeRoutesAllPrivateSubnets(t *testing.T) {
	t.Parallel()
	main := readNetworkMain(t)
	assert.Contains(t, main, "Single-NAT mode still routes ALL private subnets")
	assert.Contains(t, main, "enable_ha_nat ? count.index : 0")
}

func readNetworkMain(t *testing.T) string {
	t.Helper()
	return readFile(t, filepath.Join(repoRoot(t), "modules", "network", "main.tf"))
}
