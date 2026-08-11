package test

import (
	"path/filepath"
	"testing"

	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestAuroraReplicaModuleFilesExist(t *testing.T) {
	t.Parallel()
	root := repoRoot(t)
	for _, rel := range []string{
		"modules/aurora-replica/main.tf",
		"modules/aurora-replica/parameter_group.tf",
		"environments/dev/aurora_replica.tf",
		"environments/tst/aurora_replica.tf",
		"environments/prd/aurora_replica.tf",
	} {
		require.FileExists(t, filepath.Join(root, rel), rel)
	}
}

func TestAuroraReplicaModuleSourceContract(t *testing.T) {
	t.Parallel()
	root := repoRoot(t)
	main := readFile(t, filepath.Join(root, "modules/aurora-replica/main.tf"))
	assert.Contains(t, main, `promotion_tier          = 15`)
	assert.Contains(t, main, `AuroraReplicaLag`)

	pg := readFile(t, filepath.Join(root, "modules/aurora-replica/parameter_group.tf"))
	assert.Contains(t, pg, `hot_standby_feedback`)
	assert.Contains(t, pg, `statement_timeout`)
	assert.Contains(t, pg, `idle_in_transaction_session_timeout`)
}

func TestDatabaseModuleNoLongerOwnsReader(t *testing.T) {
	t.Parallel()
	main := readFile(t, filepath.Join(repoRoot(t), "modules/database/main.tf"))
	assert.NotContains(t, main, `aws_rds_cluster_instance" "reader"`)
}

func TestAuroraReplicaTerraformValidateAllEnvironments(t *testing.T) {
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
