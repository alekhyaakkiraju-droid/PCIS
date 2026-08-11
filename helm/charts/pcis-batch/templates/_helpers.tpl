{{/*
Expand the name of the chart.
*/}}
{{- define "pcis-batch.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "pcis-batch.fullname" -}}
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

{{- define "pcis-batch.labels" -}}
helm.sh/chart: {{ include "pcis-batch.chart" . }}
app.kubernetes.io/name: {{ include "pcis-batch.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: pcis
app.kubernetes.io/component: batch
{{- end }}

{{- define "pcis-batch.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "pcis-batch.podSecurityContext" -}}
runAsNonRoot: true
runAsUser: {{ .Values.global.securityContext.runAsUser | default 1000 }}
runAsGroup: {{ .Values.global.securityContext.runAsGroup | default 1000 }}
fsGroup: {{ .Values.global.securityContext.fsGroup | default 1000 }}
seccompProfile:
  type: RuntimeDefault
{{- end }}

{{- define "pcis-batch.containerSecurityContext" -}}
runAsNonRoot: true
readOnlyRootFilesystem: true
allowPrivilegeEscalation: false
capabilities:
  drop:
    - ALL
{{- end }}
