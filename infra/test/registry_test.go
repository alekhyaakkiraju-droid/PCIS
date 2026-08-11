package test

import (
	"path/filepath"
	"testing"

	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestRegistryModuleFilesExist(t *testing.T) {
	t.Parallel()
	root := repoRoot(t)
	for _, rel := range []string{
		"modules/registry/main.tf",
		"modules/registry/variables.tf",
		"modules/registry/outputs.tf",
		"modules/registry/README.md",
		"environments/dev/registry.tf",
		"environments/tst/registry.tf",
		"environments/prd/registry.tf",
	} {
		require.FileExists(t, filepath.Join(root, rel), rel)
	}
}

func TestRegistryModuleSourceContract(t *testing.T) {
	t.Parallel()
	root := repoRoot(t)
	main := readFile(t, filepath.Join(root, "modules/registry/main.tf"))
	assert.Contains(t, main, `aws_ecr_repository" "service"`)
	assert.Contains(t, main, `scan_on_push = true`)
	assert.Contains(t, main, `image_tag_mutability = "IMMUTABLE"`)
	assert.Contains(t, main, `aws_ecr_lifecycle_policy" "service"`)
	assert.Contains(t, main, `imageCountMoreThan`)
	assert.Contains(t, main, `tagStatus   = "untagged"`)
	assert.Contains(t, main, `aws_ecr_repository_policy" "cross_account_pull"`)
	assert.Contains(t, main, `ECR Image Scan`)
	assert.Contains(t, main, `aws_cloudwatch_event_rule" "ecr_scan_findings"`)

	vars := readFile(t, filepath.Join(root, "modules/registry/variables.tf"))
	assert.Contains(t, vars, `"pcis-base-java21"`)
	assert.Contains(t, vars, `length(name) <= 200`)
}

func TestRegistryTfvarsFixtures(t *testing.T) {
	t.Parallel()
	for _, env := range []string{"dev", "tst", "prd"} {
		body := readEnvTfvars(t, env)
		assert.Contains(t, body, `production_account_id`)
		assert.Contains(t, body, `ecr_repository_names`)
		assert.Contains(t, body, `"customer-svc"`)
		assert.Contains(t, body, `"pcis-base-java21"`)
		assert.Contains(t, body, `ecr_tagged_image_retention_count`)
	}
}

func TestRegistryTerraformValidateAllEnvironments(t *testing.T) {
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
