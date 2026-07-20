package com.maplemetric.character.application.result;

import com.maplemetric.character.infrastructure.client.dto.CharacterEquipmentResponse;

import java.util.List;

public record GetCharacterEquipmentResult(
        String date,
        String characterGender,
        String characterClass,
        Integer presetNo,
        List<ItemEquipmentResult> itemEquipment,
        List<ItemEquipmentResult> itemEquipmentPreset1,
        List<ItemEquipmentResult> itemEquipmentPreset2,
        List<ItemEquipmentResult> itemEquipmentPreset3
) {

    public static GetCharacterEquipmentResult from(CharacterEquipmentResponse response) {
        return new GetCharacterEquipmentResult(
                response.date(),
                response.characterGender(),
                response.characterClass(),
                response.presetNo(),
                convertItems(response.itemEquipment()),
                convertItems(response.itemEquipmentPreset1()),
                convertItems(response.itemEquipmentPreset2()),
                convertItems(response.itemEquipmentPreset3())
        );
    }

    private static List<ItemEquipmentResult> convertItems(List<CharacterEquipmentResponse.ItemEquipment> items) {
        if (items == null) {
            return null;
        }

        return items.stream()
                .map(item -> ItemEquipmentResult.from(item))
                .toList();
    }


    public record ItemEquipmentResult(
            String itemEquipmentPart,
            String itemEquipmentSlot,
            String itemName,
            String itemIcon,
            String itemDescription,
            String itemShapeName,
            String itemShapeIcon,
            String itemGender,
            ItemOptionResult itemTotalOption,
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
            ItemOptionResult itemAddOption,
            ItemOptionResult itemEtcOption,
            ItemOptionResult itemStarforceOption,
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
        public static ItemEquipmentResult from(CharacterEquipmentResponse.ItemEquipment item) {
            return new ItemEquipmentResult(
                    item.itemEquipmentPart(),
                    item.itemEquipmentSlot(),
                    item.itemName(),
                    item.itemIcon(),
                    item.itemDescription(),
                    item.itemShapeName(),
                    item.itemShapeIcon(),
                    item.itemGender(),
                    ItemOptionResult.from(item.itemTotalOption()),
                    item.equipmentLevelIncrease(),
                    item.growthExp(),
                    item.growthLevel(),
                    item.scrollUpgrade(),
                    item.cuttableCount(),
                    item.goldenHammerFlag(),
                    item.scrollResilienceCount(),
                    item.scrollUpgradeableCount(),
                    item.soulName(),
                    item.soulOption(),
                    ItemOptionResult.from(item.itemAddOption()),
                    ItemOptionResult.from(item.itemEtcOption()),
                    ItemOptionResult.from(item.itemStarforceOption()),
                    item.starforce(),
                    item.starforceScrollFlag(),
                    item.potentialOptionGrade(),
                    item.additionalPotentialOptionGrade(),
                    item.potentialOption1(),
                    item.potentialOption2(),
                    item.potentialOption3(),
                    item.additionalPotentialOption1(),
                    item.additionalPotentialOption2(),
                    item.additionalPotentialOption3(),
                    item.specialRingLevel(),
                    item.dateExpire()
            );
        }
    }

    public record ItemOptionResult(
            String str,
            String dex,
            String intelligence,
            String luk,
            String maxHp,
            String maxMp,
            String attackPower,
            String megicPower,
            String armor,
            String speed,
            String jump,
            String bossDemage,
            String ignoreMonsterArmor,
            String allStat,
            String demage,
            Integer equipmentLevelDecrease,
            String maxHpRate,
            String amxMpRate
    ) {

        public static ItemOptionResult from(CharacterEquipmentResponse.ItemOption option) {
            if (option == null) {
                return null;
            }

            return new ItemOptionResult(
                    option.str(),
                    option.dex(),
                    option.intelligence(),
                    option.luk(),
                    option.maxHp(),
                    option.maxMp(),
                    option.attackPower(),
                    option.magicPower(),
                    option.armor(),
                    option.speed(),
                    option.jump(),
                    option.bossDamage(),
                    option.ignoreMonsterArmor(),
                    option.allStat(),
                    option.damage(),
                    option.equipmentLevelDecrease(),
                    option.maxHpRate(),
                    option.maxMpRate()
            );
        }
    }

}

