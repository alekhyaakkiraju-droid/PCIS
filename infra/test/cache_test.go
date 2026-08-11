package test

import (
	"path/filepath"
	"testing"

	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestCacheModuleFilesExist(t *testing.T) {
	t.Parallel()
	root := repoRoot(t)
	for _, rel := range []string{
		"modules/cache/main.tf",
		"modules/cache/variables.tf",
		"modules/cache/outputs.tf",
		"modules/cache/README.md",
		"environments/dev/cache.tf",
		"environments/tst/cache.tf",
		"environments/prd/cache.tf",
	} {
		require.FileExists(t, filepath.Join(root, rel), rel)
	}
}

func TestCacheModuleSourceContract(t *testing.T) {
	t.Parallel()
	root := repoRoot(t)
	main := readFile(t, filepath.Join(root, "modules/cache/main.tf"))
	assert.Contains(t, main, `aws_elasticache_replication_group" "redis"`)
	assert.Contains(t, main, `transit_encryption_enabled = true`)
	assert.Contains(t, main, `at_rest_encryption_enabled = true`)
	assert.Contains(t, main, `ignore_changes = [auth_token]`)
	assert.Contains(t, main, `from_port   = 6379`)
	assert.Contains(t, main, `cluster-enabled`)

	dev := readEnvTfvars(t, "dev")
	prd := readEnvTfvars(t, "prd")
	assert.Contains(t, dev, `redis_node_type                = "cache.t3.medium"`)
	assert.Contains(t, dev, `redis_cluster_mode_enabled     = false`)
	assert.Contains(t, prd, `redis_node_type                = "cache.r6g.large"`)
	assert.Contains(t, prd, `redis_cluster_mode_enabled     = true`)
}

func TestCacheTerraformValidateAllEnvironments(t *testing.T) {
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
