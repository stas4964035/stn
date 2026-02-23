-- PostgreSQL
-- Initial schema, canonical entities for MVP.

-- =========================
-- 1) ENUM types
-- =========================

CREATE TYPE account_status_enum AS ENUM ('ACTIVE', 'BLOCKED', 'DELETED');

CREATE TYPE system_role_enum AS ENUM ('ADMIN', 'MODERATOR', 'USER');

CREATE TYPE marker_role_restriction_enum AS ENUM ('ANY_MEMBER', 'COMMANDER_ONLY');

CREATE TYPE marker_uniqueness_policy_enum AS ENUM ('NONE', 'ONE_PER_USER', 'ONE_PER_SQUAD');

CREATE TYPE order_status_enum AS ENUM ('CREATED', 'IN_PROGRESS', 'COMPLETED');

-- =========================
-- 2) Core tables
-- =========================

-- users
CREATE TABLE users (
                       id            BIGSERIAL PRIMARY KEY,

                       email         VARCHAR(320) NOT NULL,
                       nickname      VARCHAR(64)  NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,

                       system_role   system_role_enum    NOT NULL DEFAULT 'USER',
                       account_status account_status_enum NOT NULL DEFAULT 'ACTIVE',

                       avatar_icon   VARCHAR(64) NULL,

                       is_alive      BOOLEAN NOT NULL DEFAULT TRUE,

                       created_at    TIMESTAMPTZ NOT NULL,
                       updated_at    TIMESTAMPTZ NOT NULL,

                       CONSTRAINT uq_users_email UNIQUE (email)
);

-- companies
CREATE TABLE companies (
                           id          BIGSERIAL PRIMARY KEY,

                           name        VARCHAR(128) NOT NULL,
                           description TEXT NULL,

                           is_open     BOOLEAN NOT NULL DEFAULT TRUE,

                           created_at  TIMESTAMPTZ NOT NULL,
                           updated_at  TIMESTAMPTZ NOT NULL
);

-- squads
-- commander constraint (must be a member) is added later after squad_members is created
CREATE TABLE squads (
                        id               BIGSERIAL PRIMARY KEY,

                        name             VARCHAR(128) NOT NULL,
                        description      TEXT NULL,

                        commander_user_id BIGINT NOT NULL,

                        company_id        BIGINT NULL,

                        is_open          BOOLEAN NOT NULL DEFAULT TRUE,

                        color            VARCHAR(32) NOT NULL,

                        created_at       TIMESTAMPTZ NOT NULL,
                        updated_at       TIMESTAMPTZ NOT NULL,

                        CONSTRAINT fk_squads_company
                            FOREIGN KEY (company_id)
                                REFERENCES companies(id)
                                ON DELETE SET NULL
);

-- squad_members
-- Invariant: "user максимум в одном отряде" enforced by UNIQUE(user_id).
CREATE TABLE squad_members (
                               squad_id  BIGINT NOT NULL,
                               user_id   BIGINT NOT NULL,

                               joined_at TIMESTAMPTZ NOT NULL,

                               CONSTRAINT pk_squad_members PRIMARY KEY (squad_id, user_id),
                               CONSTRAINT uq_squad_members_user UNIQUE (user_id),

                               CONSTRAINT fk_squad_members_squad
                                   FOREIGN KEY (squad_id)
                                       REFERENCES squads(id),

                               CONSTRAINT fk_squad_members_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(id)
);

-- Invariant: "ровно один командир у отряда (и он участник)"
-- Enforced by NOT NULL commander_user_id + FK (squad_id, commander_user_id) -> squad_members(squad_id, user_id)
ALTER TABLE squads
    ADD CONSTRAINT fk_squads_commander_membership
        FOREIGN KEY (id, commander_user_id)
            REFERENCES squad_members(squad_id, user_id);

-- tactical_marker_types
CREATE TABLE tactical_marker_types (
                                       id                      BIGSERIAL PRIMARY KEY,

                                       key                     VARCHAR(64) NOT NULL,
                                       name                    VARCHAR(128) NOT NULL,

                                       default_description      TEXT NULL,
                                       icon                    VARCHAR(128) NOT NULL,

                                       default_lifetime_seconds INTEGER NULL,

                                       role_restriction         marker_role_restriction_enum NOT NULL,
                                       can_send_to_company      BOOLEAN NOT NULL,
                                       uniqueness_policy        marker_uniqueness_policy_enum NOT NULL,

                                       active                  BOOLEAN NOT NULL,

                                       created_at              TIMESTAMPTZ NOT NULL,
                                       updated_at              TIMESTAMPTZ NOT NULL
);

-- markers (append-only; deactivation = expires_at = now(UTC))
CREATE TABLE markers (
                         id              BIGSERIAL PRIMARY KEY,

                         marker_type_id  BIGINT NOT NULL,
                         creator_id      BIGINT NOT NULL,

    -- ON DELETE SET NULL is required for squad/company links, so these columns are nullable
                         squad_id        BIGINT NULL,
                         company_id      BIGINT NULL,

                         lat             NUMERIC(9,6) NOT NULL,
                         lon             NUMERIC(9,6) NOT NULL,

                         description     TEXT NULL,

                         send_to_company BOOLEAN NOT NULL DEFAULT FALSE,

                         expires_at      TIMESTAMPTZ NULL,
                         created_at      TIMESTAMPTZ NOT NULL,

                         CONSTRAINT chk_markers_lat CHECK (lat BETWEEN -90 AND 90),
                         CONSTRAINT chk_markers_lon CHECK (lon BETWEEN -180 AND 180),

                         CONSTRAINT fk_markers_squad
                             FOREIGN KEY (squad_id)
                                 REFERENCES squads(id)
                                 ON DELETE SET NULL,

                         CONSTRAINT fk_markers_company
                             FOREIGN KEY (company_id)
                                 REFERENCES companies(id)
                                 ON DELETE SET NULL,

                         CONSTRAINT fk_markers_creator
                             FOREIGN KEY (creator_id)
                                 REFERENCES users(id),

                         CONSTRAINT fk_markers_marker_type
                             FOREIGN KEY (marker_type_id)
                                 REFERENCES tactical_marker_types(id)
);

-- orders
CREATE TABLE orders (
                        id           BIGSERIAL PRIMARY KEY,

                        squad_id     BIGINT NOT NULL,
                        creator_id   BIGINT NOT NULL,

                        title        VARCHAR(256) NOT NULL,
                        text         TEXT NOT NULL,

                        status       order_status_enum NOT NULL,

                        created_at   TIMESTAMPTZ NOT NULL,
                        updated_at   TIMESTAMPTZ NOT NULL,
                        completed_at TIMESTAMPTZ NULL,

                        CONSTRAINT fk_orders_squad
                            FOREIGN KEY (squad_id)
                                REFERENCES squads(id)
                                ON DELETE CASCADE,

                        CONSTRAINT fk_orders_creator
                            FOREIGN KEY (creator_id)
                                REFERENCES users(id)
);

-- user_geo_locations (append-only history)
CREATE TABLE user_geo_locations (
                                    id          BIGSERIAL PRIMARY KEY,

                                    user_id     BIGINT NOT NULL,

                                    lat         NUMERIC(9,6) NOT NULL,
                                    lon         NUMERIC(9,6) NOT NULL,

                                    recorded_at TIMESTAMPTZ NOT NULL,

                                    created_at  TIMESTAMPTZ NOT NULL,
                                    updated_at  TIMESTAMPTZ NOT NULL,

                                    CONSTRAINT chk_user_geo_locations_lat CHECK (lat BETWEEN -90 AND 90),
                                    CONSTRAINT chk_user_geo_locations_lon CHECK (lon BETWEEN -180 AND 180),

                                    CONSTRAINT fk_user_geo_locations_user
                                        FOREIGN KEY (user_id)
                                            REFERENCES users(id)
);

-- Required index from canon
CREATE INDEX idx_user_geo_locations_user_recorded_at_desc
    ON user_geo_locations (user_id, recorded_at DESC);