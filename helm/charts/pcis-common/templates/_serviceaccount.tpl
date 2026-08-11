{{- define "pcis-common.serviceaccount" -}}
{{- if .Values.serviceAccount.create | default true }}
apiVersion: v1
kind: ServiceAccount
metadata:
  name: {{ include "pcis-common.serviceAccountName" . }}
  labels:
    {{- include "pcis-common.labels" . | nindent 4 }}
  {{- with .Values.serviceAccount.annotations }}
  annotations:
    {{- toYaml . | nindent 4 }}
  {{- end }}
automountServiceAccountToken: {{ .Values.serviceAccount.automount | default false }}
{{- end }}
{{- end }}
