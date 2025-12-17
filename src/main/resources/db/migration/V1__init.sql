SET search_path TO ${schema};

CREATE TABLE users (
                            id BIGSERIAL PRIMARY KEY,
                            object_id UUID NOT NULL UNIQUE,
                            checksum BYTEA NOT NULL,
                            lastseen TIMESTAMP
);
CREATE UNIQUE INDEX uq_entra_user_object_id ON users (object_id);

CREATE TABLE usersExternal (
                       id BIGSERIAL PRIMARY KEY,
                       object_id UUID NOT NULL UNIQUE,
                       checksum BYTEA NOT NULL,
                       lastseen TIMESTAMP
);
CREATE UNIQUE INDEX uq_entra_usersExternal_object_id ON usersExternal (object_id);

CREATE TABLE groups (
    id BIGSERIAL PRIMARY KEY,
    kafka_key TEXT NOT NULL UNIQUE,        -- stabil nøkkel fra Kafka (ikke Entra)
    entra_object_id UUID UNIQUE,           -- settes når gruppa finnes/opprettes i Entra
    kafka_checksum BYTEA NOT NULL,
    name TEXT,
    lastseen TIMESTAMP
);

CREATE INDEX idx_groups_entra_object_id ON groups(entra_object_id);

CREATE TABLE memberships (
                        id BIGSERIAL PRIMARY KEY,
                        object_id BYTEA NOT NULL UNIQUE,
                        checksum BYTEA NOT NULL,
                        lastseen TIMESTAMP

);
CREATE UNIQUE INDEX uq_entra_memberships ON memberships (object_id);

CREATE TABLE delta (
                             id BIGSERIAL PRIMARY KEY,
                             object_id VARCHAR NOT NULL UNIQUE,
                             url VARCHAR NOT NULL UNIQUE,
                             updated TIMESTAMP

);
CREATE UNIQUE INDEX uq_graph_delta ON delta (object_id);

--INSERT INTO entra_user (aad_id, body_hash) VALUES (?, ?) RETURNING id;

ALTER INDEX uq_users_object_id SET (fillfactor = 80);
REINDEX INDEX uq_users_object_id;