-- Local development databases for PCIS domain services (docker-compose.local.yml)
-- Kept in sync with helm/charts/pcis-platform/templates/postgresql-configmap.yaml
CREATE USER pcis WITH PASSWORD 'pcis' CREATEDB;

CREATE DATABASE pcis OWNER pcis;
CREATE DATABASE pcis_customer OWNER pcis;
CREATE DATABASE pcis_authz OWNER pcis;
CREATE DATABASE pcis_audit OWNER pcis;
CREATE DATABASE pcis_billing OWNER pcis;
CREATE DATABASE pcis_claims OWNER pcis;
CREATE DATABASE pcis_policy OWNER pcis;
CREATE DATABASE pcis_premium OWNER pcis;
CREATE DATABASE pcis_config OWNER pcis;
CREATE DATABASE pcis_recon OWNER pcis;
CREATE DATABASE pcis_sync OWNER pcis;
CREATE DATABASE pcis_reporting OWNER pcis;

GRANT ALL PRIVILEGES ON DATABASE pcis TO pcis;
GRANT ALL PRIVILEGES ON DATABASE pcis_customer TO pcis;
GRANT ALL PRIVILEGES ON DATABASE pcis_authz TO pcis;
GRANT ALL PRIVILEGES ON DATABASE pcis_audit TO pcis;
GRANT ALL PRIVILEGES ON DATABASE pcis_billing TO pcis;
GRANT ALL PRIVILEGES ON DATABASE pcis_claims TO pcis;
GRANT ALL PRIVILEGES ON DATABASE pcis_policy TO pcis;
GRANT ALL PRIVILEGES ON DATABASE pcis_premium TO pcis;
GRANT ALL PRIVILEGES ON DATABASE pcis_config TO pcis;
GRANT ALL PRIVILEGES ON DATABASE pcis_recon TO pcis;
GRANT ALL PRIVILEGES ON DATABASE pcis_sync TO pcis;
GRANT ALL PRIVILEGES ON DATABASE pcis_reporting TO pcis;
