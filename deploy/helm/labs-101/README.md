# labs-101 Helm Chart

Frontend, backend and PostgreSQL. Login goes through the shared Zitadel from the infra project
(`helm/auth`), which has to be installed first; `auth.issuer` points to it.

In production the chart is deployed by Argo CD from the [`infra`](https://github.com/Dannyyy101/infra) repo:
the workflow `.github/workflows/images.yaml` writes the image tags of every build into
`values/labs-101.yaml` there. A manual installation is only needed outside of that setup.

## Prerequisites

- Ingress controller (default: ingress-nginx)
- TLS secrets `labs-101-tls-frontend|backend`, e.g. via cert-manager:
  `--set-string ingress.annotations."cert-manager\.io/cluster-issuer"=letsencrypt`
- Images in the registry: `ghcr.io/dannyyy101/labs-101-backend` and `ghcr.io/dannyyy101/labs-101-frontend`
  (both built by the workflow `images.yaml`, tagged with the commit SHA and `latest`)

## Installing

Write the secrets to an uncommitted file `values-prod.local.yaml`:

```yaml
secrets:
  postgresUsername: labs
  postgresPassword: ...
  authClientId: ...
  authClientSecret: ...
  authSecret: ...                # at least 32 characters
  geminiApiKey: ...              # optional
  calendarEncryptionKey: ...     # optional, openssl rand -base64 32
auth:
  zitadelOrgId: ...
```

```sh
helm upgrade --install labs-101 deploy/helm/labs-101 -n labs-101 --create-namespace \
  -f deploy/helm/labs-101/values-prod.local.yaml
```

Alternatively, create your own secret with the same keys (`POSTGRES_USERNAME`, `POSTGRES_PASSWORD`,
`AUTH_SECRET`, ...) and set `secrets.existingSecret`.

To deploy a specific version: `--set backend.image.tag=<commit-sha>`.
