CREATE TABLE p_character_equipment_snapshot
(
    character_equipment_snapshot_id UUID         NOT NULL DEFAULT gen_random_uuid(),
    character_snapshot_id           UUID         NOT NULL,
    equipment_preset_no             SMALLINT     NOT NULL DEFAULT 1,
    slot_name                       VARCHAR(50)  NOT NULL,
    item_name                       VARCHAR(100) NOT NULL,
    item_icon_url                   TEXT,
    item_description                TEXT,
    item_level                      INTEGER,
    starforce                       INTEGER,
    potential_grade                 VARCHAR(30),
    additional_potential_grade      VARCHAR(30),
    equipment_data                  JSONB        NOT NULL,
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_p_character_equipment_snapshot
        PRIMARY KEY (character_equipment_snapshot_id),
    CONSTRAINT fk_p_character_equipment_snapshot_character
        FOREIGN KEY (character_snapshot_id)
            REFERENCES p_character_snapshot (character_snapshot_id),
    CONSTRAINT uk_p_character_equipment_snapshot_slot
        UNIQUE (
            character_snapshot_id,
            equipment_preset_no,
            slot_name
        ),
    CONSTRAINT ck_p_character_equipment_snapshot_preset
        CHECK (equipment_preset_no BETWEEN 1 AND 3),
    CONSTRAINT ck_p_character_equipment_snapshot_item_level
        CHECK (item_level IS NULL OR item_level >= 0),
    CONSTRAINT ck_p_character_equipment_snapshot_starforce
        CHECK (starforce IS NULL OR starforce >= 0)
);

CREATE INDEX idx_p_character_equipment_character
    ON p_character_equipment_snapshot (
        character_snapshot_id,
        equipment_preset_no
    );
