{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "resesok-trip.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "resesok-trip.fullname" -}}
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
Create chart name and version as used by the chart label.
*/}}
{{- define "resesok-trip.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "resesok-trip.labels" -}}
helm.sh/chart: {{ include "resesok-trip.chart" . }}
aadpodidbinding: {{ .Values.azureIdentity.bindingName  }}
app: {{ include "resesok-trip.name" . }}
{{ include "resesok-trip.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "resesok-trip.selectorLabels" -}}
app.kubernetes.io/name: {{ include "resesok-trip.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "resesok-trip.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "resesok-trip.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Template configuration for resesok-trip pod
*/}}
{{- define "resesok-trip.template"}}
metadata:
  {{- with .Values.podAnnotations }}
  annotations:
  {{- toYaml . | nindent 4 }}
  {{- end }}
  labels:
  {{- include "resesok-trip.labels" . | nindent 4 }}
spec:
  {{- with .Values.imagePullSecrets }}
  imagePullSecrets:
    {{- toYaml . | nindent 4 }}
  {{- end }}
  serviceAccountName: {{ include "resesok-trip.serviceAccountName" . }}
  securityContext:
    {{- toYaml .Values.podSecurityContext | nindent 4 }}
  containers:
    - name: {{ .Chart.Name }}
      securityContext:
        {{- toYaml .Values.securityContext | nindent 8 }}
      image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
      imagePullPolicy: {{ .Values.image.pullPolicy }}
      env:
        - name: "ENVIRONMENT"
          value: {{ .Values.ENVIRONMENT | quote }}
        - name: "OTP_SERVER_HOST"
          value: {{ .Values.OTP_SERVER_HOST | quote }}
        - name: "OTP_SERVER_PORT"
          value: {{ .Values.OTP_SERVER_PORT | quote }}
        - name: "OTP_SERVER_PROTOCOL"
          value: {{ .Values.OTP_SERVER_PROTOCOL | quote }}
{{/*         todo what should w do with this?  */}}
{{/*        - name: "ResesokProxyKeyVaultUrl"*/}}
{{/*          valueFrom:*/}}
{{/*            secretKeyRef:*/}}
{{/*              key:  ResesokProxyKeyVaultUrl*/}}
{{/*              name: {{ .Release.Name }}-auth*/}}
{{/*      {{- end }}*/}}
      ports:
        - name: http
          containerPort: {{ .Values.service.port }}
          protocol: TCP
      livenessProbe:
        failureThreshold: 120
        initialDelaySeconds: 120
        periodSeconds: 30
        successThreshold: 1
        timeoutSeconds: 30
        httpGet:
          path: /status?api-version=1
          port: http
      readinessProbe:
        failureThreshold: 120
        initialDelaySeconds: 120
        periodSeconds: 30
        successThreshold: 1
        timeoutSeconds: 30
        httpGet:
          path: /status?api-version=1
          port: http
      volumeMounts: []
      resources:
        {{- toYaml .Values.resources | nindent 8 }}
    - name: {{ .Chart.Name }}-otp
      securityContext:
        {{- toYaml .Values.securityContext | nindent 8 }}
      image: "{{ .Values.combinedServices.image.repository }}:{{ .Values.combinedServices.otpVersion }}"
      imagePullPolicy: {{ .Values.combinedServices.image.pullPolicy }}
      env:
        - name: "ENVIRONMENT"
          value: {{ .Values.ENVIRONMENT }}
        - name: "OtpKeyVaultName"
          valueFrom:
            secretKeyRef:
              key:  OtpKeyVaultName
              name: {{ .Release.Name }}-auth
      ports:
        - name: http
          containerPort: 8080
          protocol: TCP
      livenessProbe:
        failureThreshold: 120
        httpGet:
          path: /otp/routers/ready
          port: http
        initialDelaySeconds: 120
        periodSeconds: 30
        successThreshold: 1
        timeoutSeconds: 30
      readinessProbe:
        failureThreshold: 120
        httpGet:
          path: otp/routers/ready
          port: http
        initialDelaySeconds: 120
        periodSeconds: 30
        successThreshold: 1
        timeoutSeconds: 20
      volumeMounts:
        {{- range .Values.resesokOtpConfigMapMounts }}
        - name: {{ .name }}
          mountPath: {{ .mountPath }}
          subPath: {{ .subPath }}
          readOnly: {{ .readOnly }}
      {{- end }}
      resources:
        {{- toYaml .Values.combinedServices.otpResources | nindent 8 }}
  {{- end }}
  {{- with .Values.nodeSelector }}
  nodeSelector:
    {{- toYaml . | nindent 4 }}
  {{- end }}
  {{- with .Values.affinity }}
  affinity:
    {{- toYaml . | nindent 4 }}
  {{- end }}
  {{- with .Values.tolerations }}
  tolerations:
    {{- toYaml . | nindent 4 }}
  {{- end }}
  volumes:
  {{- range .Values.configmapVolumes }}
    - name: {{ .name }}
      configMap:
        name: {{ .configMap }}
  {{- range .items }}
        items:
        - key: {{ .key }}
          path: {{ .path }}
  {{- end }}
  {{- end }}