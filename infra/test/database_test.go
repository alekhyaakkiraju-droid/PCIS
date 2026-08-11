package test

import (
	"path/filepath"
	"testing"

	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestDatabaseModuleFilesExist(t *testing.T) {
	t.Parallel()
	root := repoRoot(t)
	required := []string{
		"modules/database/main.tf",
		"modules/database/variables.tf",
		"modules/database/outputs.tf",
		"modules/database/kms.tf",
		"modules/database/parameter_group.tf",
		"modules/database/iam.tf",
		"modules/database/versions.tf",
		"modules/database/README.md",
		"environments/dev/database.tf",
		"environments/tst/database.tf",
		"environments/prd/database.tf",
	}
	for _, rel := range required {
		_, err := filepath.Abs(filepath.Join(root, rel))
		require.NoError(t, err, rel)
		require.FileExists(t, filepath.Join(root, rel), rel)
	}
}

func TestAuroraTfvarsInstanceClasses(t *testing.T) {
	t.Parallel()
	dev := readEnvTfvars(t, "dev")
	tst := readEnvTfvars(t, "tst")
	prd := readEnvTfvars(t, "prd")

	assert.Contains(t, dev, `aurora_instance_class          = "db.r6g.large"`)
	assert.Contains(t, tst, `aurora_instance_class          = "db.r6g.large"`)
	assert.Contains(t, prd, `aurora_instance_class          = "db.r6g.xlarge"`)

	assert.Contains(t, dev, `aurora_multi_az                = false`)
	assert.Contains(t, tst, `aurora_multi_az                = false`)
	assert.Contains(t, prd, `aurora_multi_az                = true`)

	assert.Contains(t, prd, `aurora_deletion_protection     = true`)
	assert.Contains(t, prd, `aurora_backup_retention_period = 35`)
}

func TestDatabaseModuleSourceContract(t *testing.T) {
	t.Parallel()
	root := repoRoot(t)

	main := readFile(t, filepath.Join(root, "modules/database/main.tf"))
	assert.Contains(t, main, `engine             = "aurora-postgresql"`)
	assert.Contains(t, main, `storage_encrypted                   = true`)
	assert.Contains(t, main, `iam_database_authentication_enabled = true`)
	assert.Contains(t, main, `backup_retention_period`)
	assert.Contains(t, main, `promotion_tier = 0`)
	assert.Contains(t, main, `from_port   = 5432`)
	assert.Contains(t, main, `manage_master_user_password = true`)

	kms := readFile(t, filepath.Join(root, "modules/database/kms.tf"))
	assert.Contains(t, kms, `enable_key_rotation     = true`)
	assert.Contains(t, kms, `deletion_window_in_days = 30`)
	assert.Contains(t, kms, `alias/pcis-aurora-`)

	pg := readFile(t, filepath.Join(root, "modules/database/parameter_group.tf"))
	assert.Contains(t, pg, `family      = "aurora-postgresql17"`)
	assert.Contains(t, pg, `name         = "rds.force_ssl"`)
	assert.Contains(t, pg, `value        = "1"`)
	assert.Contains(t, pg, `name         = "timezone"`)
	assert.Contains(t, pg, `value        = "UTC"`)

	iam := readFile(t, filepath.Join(root, "modules/database/iam.tf"))
	assert.Contains(t, iam, `rds-db:connect`)
	assert.Contains(t, iam, `aws_iam_role" "db_irsa"`)

	outputs := readFile(t, filepath.Join(root, "modules/database/outputs.tf"))
	assert.Contains(t, outputs, `output "cluster_endpoint"`)
	assert.Contains(t, outputs, `output "reader_endpoint"`)
	assert.Contains(t, outputs, `output "kms_key_arn"`)
	assert.Contains(t, outputs, `output "security_group_id"`)
	assert.Contains(t, outputs, `output "irsa_role_arns"`)

	networkOut := readFile(t, filepath.Join(root, "modules/network/outputs.tf"))
	assert.Contains(t, networkOut, `output "private_app_subnet_cidrs"`)
}

func TestDatabaseUsesPrivateDataSubnets(t *testing.T) {
	t.Parallel()
	for _, env := range []string{"dev", "tst", "prd"} {
		body := readFile(t, filepath.Join(repoRoot(t), "environments", env, "database.tf"))
		assert.Contains(t, body, `private_data_subnet_ids  = module.network.private_data_subnet_ids`)
		assert.Contains(t, body, `private_app_subnet_cidrs = module.network.private_app_subnet_cidrs`)
		assert.Contains(t, body, `oidc_provider_arn = module.kubernetes.oidc_provider_arn`)
	}
}

func TestDatabaseTerraformValidateAllEnvironments(t *testing.T) {
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
