package test

import (
	"path/filepath"
	"testing"

	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestObjectStorageModuleFilesExist(t *testing.T) {
	t.Parallel()
	root := repoRoot(t)
	for _, rel := range []string{
		"modules/object-storage/main.tf",
		"modules/object-storage/variables.tf",
		"modules/object-storage/outputs.tf",
		"modules/object-storage/kms.tf",
		"modules/object-storage/iam.tf",
		"modules/object-storage/README.md",
		"environments/dev/object-storage.tf",
		"environments/tst/object-storage.tf",
		"environments/prd/object-storage.tf",
	} {
		require.FileExists(t, filepath.Join(root, rel), rel)
	}
}

func TestObjectStorageTfvarsRetention(t *testing.T) {
	t.Parallel()
	dev := readEnvTfvars(t, "dev")
	tst := readEnvTfvars(t, "tst")
	prd := readEnvTfvars(t, "prd")
	assert.Contains(t, dev, `object_lock_retention_days    = 90`)
	assert.Contains(t, tst, `object_lock_retention_days    = 180`)
	assert.Contains(t, prd, `object_lock_retention_days    = 365`)
}

func TestObjectStorageModuleSourceContract(t *testing.T) {
	t.Parallel()
	root := repoRoot(t)
	main := readFile(t, filepath.Join(root, "modules/object-storage/main.tf"))
	assert.Contains(t, main, `object_lock_enabled = true`)
	assert.Contains(t, main, `mode = "COMPLIANCE"`)
	assert.Contains(t, main, `storage_class = "GLACIER"`)
	assert.Contains(t, main, `storage_class = "DEEP_ARCHIVE"`)
	assert.Contains(t, main, `DenyPutObjectExceptAuditSvc`)
	assert.Contains(t, main, `pcis-flow-logs-`)
	assert.Contains(t, main, `pcis-terraform-state-`)

	kms := readFile(t, filepath.Join(root, "modules/object-storage/kms.tf"))
	assert.Contains(t, kms, `enable_key_rotation     = true`)
	assert.Contains(t, kms, `alias/pcis-s3-audit-`)

	iam := readFile(t, filepath.Join(root, "modules/object-storage/iam.tf"))
	assert.Contains(t, iam, `aws_iam_role" "audit_svc"`)
}

func TestObjectStorageTerraformValidateAllEnvironments(t *testing.T) {
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
