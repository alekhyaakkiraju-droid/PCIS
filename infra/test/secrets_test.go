package test

import (
	"path/filepath"
	"testing"

	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestSecretsModuleFilesExist(t *testing.T) {
	t.Parallel()
	root := repoRoot(t)
	required := []string{
		"modules/secrets/main.tf",
		"modules/secrets/variables.tf",
		"modules/secrets/outputs.tf",
		"modules/secrets/iam.tf",
		"modules/secrets/eso.tf",
		"modules/secrets/kms.tf",
		"modules/secrets/README.md",
		"modules/secrets/examples/aurora-writer-externalsecret.yaml",
		"modules/secrets/examples/reporting-aurora-reader-externalsecret.yaml",
		"environments/dev/secrets.tf",
		"environments/tst/secrets.tf",
		"environments/prd/secrets.tf",
	}
	for _, rel := range required {
		require.FileExists(t, filepath.Join(root, rel), rel)
	}
}

func TestSecretsTfvarsFixtures(t *testing.T) {
	t.Parallel()
	for _, env := range []string{"dev", "tst", "prd"} {
		body := readEnvTfvars(t, env)
		assert.Contains(t, body, `enable_external_secrets        = false`)
		assert.Contains(t, body, `create_secret_rotation_lambda  = true`)
		assert.Contains(t, body, `secret_rotation_days           = 90`)
	}
}

func TestSecretsModuleSourceContract(t *testing.T) {
	t.Parallel()
	root := repoRoot(t)

	main := readFile(t, filepath.Join(root, "modules/secrets/main.tf"))
	assert.Contains(t, main, `pcis/${var.environment_name}`)
	assert.Contains(t, main, `"aurora-writer"`)
	assert.Contains(t, main, `"aurora-reader"`)
	assert.Contains(t, main, `"keycloak-client"`)
	assert.Contains(t, main, `"kafka-sasl"`)
	assert.Contains(t, main, `automatically_after_days = 90`)
	assert.Contains(t, main, `SecretsManagerRDSPostgreSQLRotationSingleUser`)
	assert.Contains(t, main, `kms_key_id              = aws_kms_key.secrets.arn`)
	assert.NotContains(t, main, `aws_secretsmanager_secret_version`)
	assert.NotContains(t, main, `secret_string`)

	iam := readFile(t, filepath.Join(root, "modules/secrets/iam.tf"))
	assert.Contains(t, iam, `secretsmanager:GetSecretValue`)
	assert.Contains(t, iam, `aws_iam_role" "service_secrets"`)
	assert.Contains(t, iam, `aws_iam_role_policy" "service_secrets"`)

	eso := readFile(t, filepath.Join(root, "modules/secrets/eso.tf"))
	assert.Contains(t, eso, `chart      = "external-secrets"`)
	assert.Contains(t, eso, `ClusterSecretStore`)
	assert.Contains(t, eso, `aws-secrets-manager`)
	assert.Contains(t, eso, `var.enable_eso`)

	example := readFile(t, filepath.Join(root, "modules/secrets/examples/aurora-writer-externalsecret.yaml"))
	assert.Contains(t, example, `refreshInterval: 1h`)
	assert.Contains(t, example, `ClusterSecretStore`)
	assert.Contains(t, example, `pcis/dev/aurora-writer`)
}

func TestSecretsEnvWiresOIDC(t *testing.T) {
	t.Parallel()
	for _, env := range []string{"dev", "tst", "prd"} {
		body := readFile(t, filepath.Join(repoRoot(t), "environments", env, "secrets.tf"))
		assert.Contains(t, body, `oidc_provider_arn      = module.kubernetes.oidc_provider_arn`)
		assert.Contains(t, body, `enable_eso             = var.enable_external_secrets`)
	}
}

func TestSecretsTerraformValidateAllEnvironments(t *testing.T) {
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
