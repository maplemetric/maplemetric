CREATE TABLE p_overall_ranking_collection
(
    overall_ranking_collection_id UUID         NOT NULL DEFAULT gen_random_uuid(),
    snapshot_date                 DATE         NOT NULL,
    world_name                    VARCHAR(30)  NOT NULL,
    world_type                    INTEGER      NOT NULL,
    class_name                    VARCHAR(50)  NOT NULL,
    source                        VARCHAR(50)  NOT NULL,
    page_count                    INTEGER      NOT NULL,
    requested_max_pages           INTEGER      NOT NULL,
    truncated                     BOOLEAN      NOT NULL,
    sample_size                   INTEGER      NOT NULL,
    collected_at                  TIMESTAMPTZ  NOT NULL,
    created_at                    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_p_overall_ranking_collection
        PRIMARY KEY (overall_ranking_collection_id),
    CONSTRAINT uk_p_overall_ranking_collection_condition
        UNIQUE (snapshot_date, world_name, world_type, class_name),
    CONSTRAINT ck_p_overall_ranking_collection_page_count
        CHECK (page_count >= 1),
    CONSTRAINT ck_p_overall_ranking_collection_requested_max_pages
        CHECK (requested_max_pages >= 1),
    CONSTRAINT ck_p_overall_ranking_collection_sample_size
        CHECK (sample_size >= 0)
);

CREATE TABLE p_overall_ranking_snapshot
(
    overall_ranking_snapshot_id   UUID         NOT NULL DEFAULT gen_random_uuid(),
    overall_ranking_collection_id UUID         NOT NULL,
    ranking                       INTEGER      NOT NULL,
    character_name                VARCHAR(30)  NOT NULL,
    world_name                    VARCHAR(30)  NOT NULL,
    class_name                    VARCHAR(50)  NOT NULL,
    sub_class_name                VARCHAR(50),
    character_level               INTEGER      NOT NULL,
    character_exp                 BIGINT,
    character_popularity          INTEGER,
    character_guild_name          VARCHAR(50),
    created_at                    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_p_overall_ranking_snapshot
        PRIMARY KEY (overall_ranking_snapshot_id),
    CONSTRAINT fk_p_overall_ranking_snapshot_collection
        FOREIGN KEY (overall_ranking_collection_id)
            REFERENCES p_overall_ranking_collection (overall_ranking_collection_id),
    CONSTRAINT uk_p_overall_ranking_snapshot_collection_rank
        UNIQUE (overall_ranking_collection_id, ranking),
    CONSTRAINT ck_p_overall_ranking_snapshot_level
        CHECK (character_level BETWEEN 1 AND 999),
    CONSTRAINT ck_p_overall_ranking_snapshot_exp
        CHECK (character_exp IS NULL OR character_exp >= 0)
);
