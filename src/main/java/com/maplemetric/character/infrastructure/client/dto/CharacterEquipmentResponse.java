package com.maplemetric.character.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CharacterEquipmentResponse(
        String date,
        String characterGender,
        String characterClass,
        Integer presetNo,
        List<ItemEquipment> itemEquipment,

        // SnakeCaseStrategy는 끝자리 숫자 앞에 밑줄을 넣지 않아
        // itemEquipmentPreset1을 item_equipment_preset1로 변환한다.
        // Nexon 필드는 item_equipment_preset_1이라 이름이 어긋나 바인딩되지 않으므로
        // 아래 potentialOption1처럼 이름을 명시한다.
        @JsonProperty("item_equipment_preset_1")
        List<ItemEquipment> itemEquipmentPreset1,

        @JsonProperty("item_equipment_preset_2")
        List<ItemEquipment> itemEquipmentPreset2,

        @JsonProperty("item_equipment_preset_3")
        List<ItemEquipment> itemEquipmentPreset3
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ItemEquipment(
            String itemEquipmentPart,
            String itemEquipmentSlot,
            String itemName,
            String itemIcon,
            String itemDescription,
            String itemShapeName,
            String itemShapeIcon,
            String itemGender,
            ItemOption itemTotalOption,
            Integer equipmentLevelIncrease,
            String growthExp,
            Integer growthLevel,
            String scrollUpgrade,
            String cuttableCount,
            String goldenHammerFlag,
            String scrollResilienceCount,
            String scrollUpgradeableCount,
            String soulName,
            String soulOption,
            ItemOption itemAddOption,
            ItemOption itemEtcOption,
            ItemOption itemStarforceOption,
            String starforce,
            String starforceScrollFlag,
            String potentialOptionGrade,
            String additionalPotentialOptionGrade,

            @JsonProperty("potential_option_1")
            String potentialOption1,

            @JsonProperty("potential_option_2")
            String potentialOption2,

            @JsonProperty("potential_option_3")
            String potentialOption3,

            @JsonProperty("additional_potential_option_1")
            String additionalPotentialOption1,

            @JsonProperty("additional_potential_option_2")
            String additionalPotentialOption2,

            @JsonProperty("additional_potential_option_3")
            String additionalPotentialOption3,

            Integer specialRingLevel,
            String dateExpire
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ItemOption(
            String str,
            String dex,
            String intelligence,
            String luk,
            String maxHp,
            String maxMp,
            String attackPower,
            String magicPower,
            String armor,
            String speed,
            String jump,
            String bossDamage,
            String ignoreMonsterArmor,
            String allStat,
            String damage,
            Integer equipmentLevelDecrease,
            String maxHpRate,
            String maxMpRate
    ) {
    }
}