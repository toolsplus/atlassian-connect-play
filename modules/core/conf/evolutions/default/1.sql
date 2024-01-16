# --- !Ups
CREATE TABLE atlassian_host
(
    client_key                 VARCHAR PRIMARY KEY NOT NULL,
    key                        VARCHAR             NOT NULL,
    public_key                 VARCHAR             NOT NULL,
    oauth_client_id            VARCHAR,
    installation_id            VARCHAR,
    shared_secret              VARCHAR             NOT NULL,
    server_version             VARCHAR             NOT NULL,
    plugins_version            VARCHAR             NOT NULL,
    base_url                   VARCHAR             NOT NULL,
    product_type               VARCHAR             NOT NULL,
    description                VARCHAR             NOT NULL,
    service_entitlement_number VARCHAR,
    installed                  VARCHAR             NOT NULL
);
CREATE UNIQUE INDEX uq_ac_host_client_key
    ON atlassian_host (client_key);
CREATE INDEX uq_ac_host_base_url
    ON atlassian_host (base_url);
CREATE INDEX ac_host_installation_id
    ON atlassian_host (installation_id);

CREATE TABLE forge_installation
(
    installation_id VARCHAR PRIMARY KEY,
    client_key      VARCHAR NOT NULL
);
CREATE UNIQUE INDEX uq_forge_installation_installation_id
    ON forge_installation (installation_id);
ALTER TABLE forge_installation
    ADD CONSTRAINT fk_forge_installation_atlassian_host FOREIGN KEY (client_key) REFERENCES atlassian_host (client_key) ON UPDATE CASCADE ON DELETE CASCADE;

# --- !Downs
DROP TABLE forge_installation;
DROP TABLE atlassian_host;
