package test

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestKubernetesModuleFilesExist(t *testing.T) {
	t.Parallel()
	root := repoRoot(t)
	required := []string{
		"modules/kubernetes/main.tf",
		"modules/kubernetes/variables.tf",
		"modules/kubernetes/outputs.tf",
		"modules/kubernetes/iam.tf",
		"modules/kubernetes/README.md",
		"modules/kubernetes_addons/istio.tf",
		"modules/kubernetes_addons/argocd.tf",
		"modules/kubernetes_addons/namespaces.tf",
		"modules/kubernetes_addons/helm-values/istio-values.yaml",
		"modules/kubernetes_addons/helm-values/argocd-values.yaml",
		"environments/dev/kubernetes.tf",
		"environments/tst/kubernetes.tf",
		"environments/prd/kubernetes.tf",
	}
	for _, rel := range required {
		_, err := os.Stat(filepath.Join(root, rel))
		require.NoError(t, err, rel)
	}
}

func TestBatchNodeGroupScalesToZeroInTfvars(t *testing.T) {
	t.Parallel()
	for _, env := range []string{"dev", "tst", "prd"} {
		body := readEnvTfvars(t, env)
		assert.Contains(t, body, "batch_scaling")
		assert.Contains(t, body, "min_size = 0")
	}
}

func TestPrdApplicationMinTwoAndNoArgoAutoSync(t *testing.T) {
	t.Parallel()
	body := readEnvTfvars(t, "prd")
	assert.Contains(t, body, "min_size = 2")
	assert.Contains(t, body, "argocd_automated_sync      = false")
}

func TestKubernetesModuleSourceContract(t *testing.T) {
	t.Parallel()
	main := readFile(t, filepath.Join(repoRoot(t), "modules/kubernetes/main.tf"))
	assert.Contains(t, main, `node_group_name = "application"`)
	assert.Contains(t, main, `node_group_name = "batch"`)
	assert.Contains(t, main, `key    = "batch"`)
	assert.Contains(t, main, `"api"`)
	assert.Contains(t, main, `"audit"`)
	assert.Contains(t, main, `endpoint_private_access = true`)

	iam := readFile(t, filepath.Join(repoRoot(t), "modules/kubernetes/iam.tf"))
	assert.Contains(t, iam, "aws_iam_openid_connect_provider")

	istio := readFile(t, filepath.Join(repoRoot(t), "modules/kubernetes_addons/istio.tf"))
	assert.Contains(t, istio, `mode = "STRICT"`)
	assert.Contains(t, istio, "istio-base")

	ns := readFile(t, filepath.Join(repoRoot(t), "modules/kubernetes_addons/namespaces.tf"))
	assert.Contains(t, ns, "pod-security.kubernetes.io/enforce")
	assert.Contains(t, ns, "default-deny-all")
	assert.Contains(t, ns, "pcis-dev")
}

func TestHelmValuesCommitted(t *testing.T) {
	t.Parallel()
	istio := readFile(t, filepath.Join(repoRoot(t), "modules/kubernetes_addons/helm-values/istio-values.yaml"))
	assert.Contains(t, istio, "enableAutoMtls")
	argocd := readFile(t, filepath.Join(repoRoot(t), "modules/kubernetes_addons/helm-values/argocd-values.yaml"))
	assert.Contains(t, argocd, "role:org-admin")
	assert.Contains(t, argocd, "revisionHistoryLimit")
}

func TestKubernetesTerraformValidateAllEnvironments(t *testing.T) {
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

func readEnvTfvars(t *testing.T, env string) string {
	t.Helper()
	return readFile(t, filepath.Join(repoRoot(t), "environments", env, "terraform.tfvars"))
}

func readFile(t *testing.T, path string) string {
	t.Helper()
	b, err := os.ReadFile(path)
	require.NoError(t, err)
	return string(b)
}
