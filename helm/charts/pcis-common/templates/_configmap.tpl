{{- define "pcis-common.configmap" -}}
{{- if .Values.configMap.enabled | default false }}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "pcis-common.fullname" . }}
  labels:
    {{- include "pcis-common.labels" . | nindent 4 }}
data:
  {{- range $key, $value := .Values.configMap.data }}
  {{ $key }}: {{ $value | quote }}
  {{- end }}
{{- end }}
{{- end }}
