{{/*
Expand the name of the chart.
*/}}
{{- define "pcis-common.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "pcis-common.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "pcis-common.labels" -}}
helm.sh/chart: {{ include "pcis-common.chart" . }}
{{ include "pcis-common.selectorLabels" . }}
app.kubernetes.io/version: {{ .Values.image.tag | default .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: pcis
{{- end }}

{{- define "pcis-common.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "pcis-common.selectorLabels" -}}
app.kubernetes.io/name: {{ include "pcis-common.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app: {{ .Values.appName | default (include "pcis-common.name" .) }}
{{- end }}

{{/*
Service account name
*/}}
{{- define "pcis-common.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "pcis-common.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Pod security context — enforce non-root
*/}}
{{- define "pcis-common.podSecurityContext" -}}
runAsNonRoot: true
runAsUser: {{ .Values.securityContext.runAsUser | default 1000 }}
runAsGroup: {{ .Values.securityContext.runAsGroup | default 1000 }}
fsGroup: {{ .Values.securityContext.fsGroup | default 1000 }}
seccompProfile:
  type: RuntimeDefault
{{- end }}

{{/*
Container security context — harden defaults
*/}}
{{- define "pcis-common.containerSecurityContext" -}}
runAsNonRoot: true
readOnlyRootFilesystem: true
allowPrivilegeEscalation: false
capabilities:
  drop:
    - ALL
{{- end }}

{{/*
Istio injection annotations
*/}}
{{- define "pcis-common.istioAnnotations" -}}
{{- if .Values.istio.inject | default true }}
sidecar.istio.io/inject: "true"
{{- end }}
{{- end }}
