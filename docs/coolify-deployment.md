# Coolify deployment

Coolify builds `main` on the Oracle ARM64 server using `compose.coolify.yaml` and
the multi-stage Java 21 Dockerfile. GitHub Actions CI remains a test workflow;
image publishing and deployment do not require Actions or a self-hosted runner.

The Docker build runs the Firebase credential and notification unit tests.
The complete test suite, including MySQL Testcontainers, runs in CI. A push
webhook starts Coolify independently of CI, so require passing CI before merging
changes into main if CI must be a deployment gate.

## Runtime configuration

Keep DB, AuraDB and monitoring credentials in Coolify runtime environment
variables. The required names appear in `compose.coolify.yaml`; do not add their
values to this repository. MySQL and host Grafana Alloy remain independent.

Firebase credentials are a read-only file mounted from
`/opt/mapleland/secrets/firebase-service-account.json` into
`/run/secrets/firebase-service-account.json`. The host directory is root-only
0700; the file is 0400 owned by container UID 1002. The application uses
`FIREBASE_CREDENTIALS=file:/run/secrets/firebase-service-account.json`.
No credentials are needed during build. `.dockerignore` excludes legacy
Firebase files. The application retains its old classpath default only for
compatibility with previously built images.

The runtime user is 1002:1001 and logs remain under `/workspace/logs`.
Host management binding stays `127.0.0.1:18080` for Alloy. The existing HTTP
binding is retained. Hibernate validates the schema and does not apply DDL.

For parallel validation, override API_HTTP_BIND, API_MANAGEMENT_BIND and
API_LOG_DIR and disable ALRIM_EVENT_BATCH_ENABLED and AURADB_KEEP_ALIVE_ENABLED.
Stop the previous production container before enabling jobs and production
bindings to avoid duplicate scheduled jobs or port conflicts.

## Automatic deployments

The repository push webhook calls Coolify's `/webhooks/source/github/events/manual`
endpoint with a per-application HMAC secret. Coolify filters for `main` and verifies
the signature. Here, "manual" means a manually registered repository webhook,
not a manually triggered deployment. The endpoint needs a narrowly scoped
Cloudflare Access exception; the administration UI remains protected.

The public hostname keeps its direct OCI DNS record. Existing Traefik terminates
TLS and forwards to Coolify's proxy, which owns the application's routing.

Old credential-containing images are retained only for rollback. The new image
does not remove credentials from those older images; handle retention/key rotation
separately when rollback is no longer needed.
