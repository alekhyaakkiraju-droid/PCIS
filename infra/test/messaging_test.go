package test

import (
	"path/filepath"
	"testing"

	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestMessagingModuleFilesExist(t *testing.T) {
	t.Parallel()
	root := repoRoot(t)
	for _, rel := range []string{
		"modules/messaging/main.tf",
		"modules/messaging/variables.tf",
		"modules/messaging/outputs.tf",
		"modules/messaging/README.md",
		"environments/dev/messaging.tf",
		"environments/tst/messaging.tf",
		"environments/prd/messaging.tf",
	} {
		require.FileExists(t, filepath.Join(root, rel), rel)
	}
}

func TestMessagingModuleSourceContract(t *testing.T) {
	t.Parallel()
	root := repoRoot(t)
	main := readFile(t, filepath.Join(root, "modules/messaging/main.tf"))
	assert.Contains(t, main, `aws_msk_cluster" "kafka"`)
	assert.Contains(t, main, `scram = true`)
	assert.Contains(t, main, `client_broker = "TLS"`)
	assert.Contains(t, main, `auto.create.topics.enable=false`)
	assert.Contains(t, main, `from_port   = 9096`)
	assert.Contains(t, main, `cloudwatch_logs`)
	assert.Contains(t, main, `aws_msk_scram_secret_association`)

	dev := readEnvTfvars(t, "dev")
	prd := readEnvTfvars(t, "prd")
	assert.Contains(t, dev, `msk_broker_count               = 2`)
	assert.Contains(t, prd, `msk_broker_count               = 3`)
	assert.Contains(t, prd, `msk_min_insync_replicas        = 2`)
}

func TestMessagingTerraformValidateAllEnvironments(t *testing.T) {
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
