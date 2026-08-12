{{- define "pcis-common.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "pcis-common.fullname" . }}
  labels:
    {{- include "pcis-common.labels" . | nindent 4 }}
spec:
  {{- if not (and .Values.autoscaling.enabled (ne .Values.autoscaling.enableHpa false)) }}
  replicas: {{ .Values.replicaCount | default 1 }}
  {{- end }}
  selector:
    matchLabels:
      {{- include "pcis-common.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      annotations:
        {{- include "pcis-common.istioAnnotations" . | nindent 8 }}
        {{- with .Values.podAnnotations }}
        {{- toYaml . | nindent 8 }}
        {{- end }}
      labels:
        {{- include "pcis-common.selectorLabels" . | nindent 8 }}
        {{- with .Values.podLabels }}
        {{- toYaml . | nindent 8 }}
        {{- end }}
    spec:
      serviceAccountName: {{ include "pcis-common.serviceAccountName" . }}
      securityContext:
        {{- include "pcis-common.podSecurityContext" . | nindent 8 }}
      containers:
        - name: {{ .Values.appName | default (include "pcis-common.name" .) }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
          imagePullPolicy: {{ .Values.image.pullPolicy | default "IfNotPresent" }}
          securityContext:
            {{- include "pcis-common.containerSecurityContext" . | nindent 12 }}
          ports:
            - name: http
              containerPort: {{ .Values.service.targetPort | default 8080 }}
              protocol: TCP
          env:
            {{- range $key, $value := .Values.env }}
            - name: {{ $key }}
              value: {{ $value | quote }}
            {{- end }}
          {{- if .Values.envFromConfigMap }}
          envFrom:
            - configMapRef:
                name: {{ include "pcis-common.fullname" . }}
          {{- end }}
          livenessProbe:
            httpGet:
              path: {{ .Values.probes.liveness.path | default "/actuator/health/readiness" }}
              port: http
            initialDelaySeconds: {{ .Values.probes.liveness.initialDelaySeconds | default 30 }}
            periodSeconds: {{ .Values.probes.liveness.periodSeconds | default 10 }}
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: http
            initialDelaySeconds: {{ .Values.probes.readiness.initialDelaySeconds | default 10 }}
            periodSeconds: {{ .Values.probes.readiness.periodSeconds | default 5 }}
          startupProbe:
            httpGet:
              path: {{ .Values.probes.startup.path | default "/actuator/health/readiness" }}
              port: http
            failureThreshold: {{ .Values.probes.startup.failureThreshold | default 30 }}
            periodSeconds: {{ .Values.probes.startup.periodSeconds | default 10 }}
          resources:
            {{- toYaml .Values.resources | nindent 12 }}
          volumeMounts:
            - name: tmp
              mountPath: /tmp
            {{- with .Values.extraVolumeMounts }}
            {{- toYaml . | nindent 12 }}
            {{- end }}
      volumes:
        - name: tmp
          emptyDir: {}
        {{- with .Values.extraVolumes }}
        {{- toYaml . | nindent 8 }}
        {{- end }}
      {{- with .Values.nodeSelector }}
      nodeSelector:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.affinity }}
      affinity:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.tolerations }}
      tolerations:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml . | nindent 8 }}
      {{- end }}
{{- end }}
