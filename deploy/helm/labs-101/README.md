# labs-101 Helm Chart

Frontend, Backend und PostgreSQL. Die Anmeldung läuft über das gemeinsame Zitadel aus dem
Infra-Projekt (`helm/auth`), das vorher installiert sein muss; `auth.issuer` zeigt darauf.

## Voraussetzungen

- Ingress-Controller (Standard: ingress-nginx)
- TLS-Secrets `labs-101-tls-frontend|backend`, z. B. über cert-manager:
  `--set-string ingress.annotations."cert-manager\.io/cluster-issuer"=letsencrypt`
- Images in der Registry: `ghcr.io/dannyyy101/labs-101-backend` (Workflow `backend-image.yaml`)
  und `ghcr.io/dannyyy101/labs-101-frontend`

## Installieren

Secrets in eine nicht eingecheckte Datei `values-prod.local.yaml` schreiben:

```yaml
secrets:
  postgresUsername: labs
  postgresPassword: ...
  authClientId: ...
  authClientSecret: ...
  authSecret: ...                # mind. 32 Zeichen
  geminiApiKey: ...              # optional
  calendarEncryptionKey: ...     # optional, openssl rand -base64 32
auth:
  zitadelOrgId: ...
```

```sh
helm upgrade --install labs-101 deploy/helm/labs-101 -n labs-101 --create-namespace \
  -f deploy/helm/labs-101/values-prod.local.yaml
```

Alternativ ein eigenes Secret mit denselben Keys (`POSTGRES_USERNAME`, `POSTGRES_PASSWORD`,
`AUTH_SECRET`, ...) anlegen und `secrets.existingSecret` setzen.

Eine bestimmte Version deployen: `--set backend.image.tag=<commit-sha>`.
