CREATE TABLE IF NOT EXISTS edc_lease
(
    leased_by      VARCHAR NOT NULL,
    leased_at      BIGINT,
    lease_duration INTEGER NOT NULL,
    lease_id       VARCHAR NOT NULL,
    resource_kind  VARCHAR NOT NULL,
    PRIMARY KEY(lease_id, resource_kind)
);


CREATE TABLE IF NOT EXISTS edc_data_plane_instance
(
    id                   VARCHAR NOT NULL PRIMARY KEY,
    data                 JSON
);
