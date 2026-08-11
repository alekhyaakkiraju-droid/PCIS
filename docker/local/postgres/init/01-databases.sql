-- Local development databases for PCIS domain services (docker-compose.local.yml)
CREATE USER pcis WITH PASSWORD 'pcis' CREATEDB;
CREATE DATABASE pcis OWNER pcis;
CREATE DATABASE pcis_claims OWNER pcis;
CREATE DATABASE pcis_policy OWNER pcis;
CREATE DATABASE pcis_billing OWNER pcis;
GRANT ALL PRIVILEGES ON DATABASE pcis TO pcis;
GRANT ALL PRIVILEGES ON DATABASE pcis_claims TO pcis;
GRANT ALL PRIVILEGES ON DATABASE pcis_policy TO pcis;
GRANT ALL PRIVILEGES ON DATABASE pcis_billing TO pcis;
