{{- define "pcis-common.networkpolicy" -}}
{{- if .Values.networkPolicy.enabled | default true }}
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: {{ include "pcis-common.fullname" . }}
  labels:
    {{- include "pcis-common.labels" . | nindent 4 }}
spec:
  podSelector:
    matchLabels:
      {{- include "pcis-common.selectorLabels" . | nindent 6 }}
  policyTypes:
    - Ingress
    - Egress
  ingress:
    # default-deny is implied; only listed sources are allowed
    - from:
        - podSelector:
            matchLabels:
              app: api-gateway
        {{- range .Values.networkPolicy.allowedPeers }}
        - podSelector:
            matchLabels:
              app: {{ . }}
        {{- end }}
      ports:
        - protocol: TCP
          port: {{ .Values.service.targetPort | default 8080 }}
  egress:
    - {} # allow egress by default for DB/messaging; tighten per-service if needed
{{- end }}
{{- end }}
