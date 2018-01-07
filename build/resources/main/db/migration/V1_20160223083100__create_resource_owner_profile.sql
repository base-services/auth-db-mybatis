CREATE TABLE resource_owner_profile (
    id                      UUID PRIMARY KEY,
    resource_owner_id       UUID references resource_owner(uuid) NOT NULL,
    name                    varchar(245),
    middle_name             varchar(245),
    nick_name               varchar(245),
    preferred_user_name     varchar(245),
    profile                 varchar(245),
    picture                 varchar(245),
    website                 varchar(245),
    gender                  varchar(7),
    birth_date              timestamp with time zone,
    zone_info               varchar(50),
    locale                  varchar(50),
    phone_number            varchar(245),
    phone_number_verified   boolean default false NOT NULL,
    created_at              timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);