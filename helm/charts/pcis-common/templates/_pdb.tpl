{{- define "pcis-common.pdb" -}}
{{- if and (.Values.podDisruptionBudget.enabled | default true) (ne .Values.podDisruptionBudget.enablePdb false) }}
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: {{ include "pcis-common.fullname" . }}
  labels:
    {{- include "pcis-common.labels" . | nindent 4 }}
spec:
  minAvailable: {{ .Values.podDisruptionBudget.minAvailable | default 1 }}
  selector:
    matchLabels:
      {{- include "pcis-common.selectorLabels" . | nindent 6 }}
{{- end }}
{{- end }}
