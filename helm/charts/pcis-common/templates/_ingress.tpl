{{- define "pcis-common.ingress" -}}
{{- if .Values.ingress.enabled }}
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {{ include "pcis-common.fullname" . }}
  labels:
    {{- include "pcis-common.labels" . | nindent 4 }}
  {{- with .Values.ingress.annotations }}
  annotations:
    {{- toYaml . | nindent 4 }}
  {{- end }}
spec:
  {{- if .Values.ingress.className }}
  ingressClassName: {{ .Values.ingress.className }}
  {{- end }}
  rules:
    - host: {{ .Values.ingress.host | required "ingress.host is required when ingress.enabled=true" }}
      http:
        paths:
          - path: {{ .Values.ingress.path | default "/" }}
            pathType: {{ .Values.ingress.pathType | default "Prefix" }}
            backend:
              service:
                name: {{ include "pcis-common.fullname" . }}
                port:
                  number: {{ .Values.service.port | default 80 }}
{{- end }}
{{- end }}
