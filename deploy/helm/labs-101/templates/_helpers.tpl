{{- define "labs101.fullname" -}}
{{- if contains .Chart.Name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "labs101.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version }}
app.kubernetes.io/part-of: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/* usage: include "labs101.selectorLabels" (list . "backend") */}}
{{- define "labs101.selectorLabels" -}}
{{- $root := index . 0 -}}
app.kubernetes.io/name: {{ $root.Chart.Name }}
app.kubernetes.io/instance: {{ $root.Release.Name }}
app.kubernetes.io/component: {{ index . 1 }}
{{- end -}}

{{- define "labs101.secretName" -}}
{{- .Values.secrets.existingSecret | default (include "labs101.fullname" .) -}}
{{- end -}}

{{- define "labs101.postgresHost" -}}
{{- if .Values.postgres.enabled -}}
{{- printf "%s-postgres" (include "labs101.fullname" .) -}}
{{- else -}}
{{- required "postgres.host is required when postgres.enabled is false" .Values.postgres.host -}}
{{- end -}}
{{- end -}}

{{- define "labs101.authIssuer" -}}
{{- required "auth.issuer is required" .Values.auth.issuer -}}
{{- end -}}

{{/* usage: include "labs101.secretEnv" (list . "ENV_NAME" "secret-key") */}}
{{- define "labs101.secretEnv" -}}
- name: {{ index . 1 }}
  valueFrom:
    secretKeyRef:
      name: {{ include "labs101.secretName" (index . 0) }}
      key: {{ index . 2 }}
{{- end -}}

{{- define "labs101.optionalSecretEnv" -}}
- name: {{ index . 1 }}
  valueFrom:
    secretKeyRef:
      name: {{ include "labs101.secretName" (index . 0) }}
      key: {{ index . 2 }}
      optional: true
{{- end -}}
