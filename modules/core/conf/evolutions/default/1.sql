# --- !Ups
CREATE TABLE atlassian_host
(
    client_key                          VARCHAR PRIMARY KEY NOT NULL,
    key                                 VARCHAR             NOT NULL,
    oauth_client_id                     VARCHAR,
    shared_secret                       VARCHAR             NOT NULL,
    authentication_type                 VARCHAR             NOT NULL,
    cloud_id                            VARCHAR,
    base_url                            VARCHAR             NOT NULL,
    display_url                         VARCHAR,
    display_url_servicedesk_help_center VARCHAR,
    product_type                        VARCHAR             NOT NULL,
    description                         VARCHAR             NOT NULL,
    service_entitlement_number          VARCHAR,
    installed                           VARCHAR             NOT NULL
);
CREATE UNIQUE INDEX uq_ac_host_client_key
    ON atlassian_host (client_key);
CREATE UNIQUE INDEX uq_ac_host_base_url
    ON atlassian_host (base_url);

# --- !Downs
DROP TABLE atlassian_host;
