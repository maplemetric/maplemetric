package com.maplemetric.character.infrastructure.client.dto;

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
        List<ItemEquipment> itemEquipmentPreset1,
        List<ItemEquipment> itemEquipmentPreset2,
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
            String potentialOption1,
            String potentialOption2,
            String potentialOption3,
            String additionalPotentialOption1,
            String additionalPotentialOption2,
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