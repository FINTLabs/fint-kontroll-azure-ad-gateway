SET search_path TO ${schema};

CREATE TABLE users (
                            id BIGSERIAL PRIMARY KEY,
                            object_id UUID NOT NULL UNIQUE,
                            checksum BYTEA NOT NULL,
                            lastseen TIMESTAMP
);
CREATE UNIQUE INDEX uq_entra_user_object_id ON users (id) INCLUDE (object_id);

CREATE TABLE usersExternal (
                       id BIGSERIAL PRIMARY KEY,
                       object_id UUID NOT NULL UNIQUE,
                       checksum BYTEA NOT NULL,
                       lastseen TIMESTAMP
);
CREATE UNIQUE INDEX uq_entra_usersExternal_object_id ON usersExternal (id) INCLUDE (object_id);

CREATE TABLE groups (
                       id BIGSERIAL PRIMARY KEY,
                       object_id UUID NOT NULL UNIQUE,
                       kafkaChecksum BYTEA NOT NULL,
                       entraChecksum BYTEA NOT NULL,
                       name VARCHAR,
                       lastseen TIMESTAMP
);
CREATE UNIQUE INDEX uq_entra_group_object_id ON groups (id) INCLUDE (object_id);

CREATE TABLE memberships (
                        id BIGSERIAL PRIMARY KEY,
                        object_id BYTEA NOT NULL UNIQUE,
                        checksum BYTEA NOT NULL,
                        lastseen TIMESTAMP

);
CREATE UNIQUE INDEX uq_entra_memberships ON memberships (id) INCLUDE (object_id);

CREATE TABLE delta (
                             id BIGSERIAL PRIMARY KEY,
                             object_id VARCHAR NOT NULL UNIQUE,
                             url VARCHAR NOT NULL UNIQUE,
                             updated TIMESTAMP

);
CREATE UNIQUE INDEX uq_graph_delta ON delta (id) INCLUDE (object_id);

"INSERT INTO entra_user (aad_id, body_hash) VALUES (?, ?) RETURNING id";