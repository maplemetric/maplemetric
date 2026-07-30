package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterEquipmentResult;

import java.util.List;

public record GetCharacterEquipmentResponse(
        String date,
        String characterGender,
        String characterClass,
        Integer presetNo,
        List<ItemEquipmentResponse> itemEquipment,
        List<ItemEquipmentResponse> itemEquipmentPreset1,
        List<ItemEquipmentResponse> itemEquipmentPreset2,
        List<ItemEquipmentResponse> itemEquipmentPreset3
) {

    public static GetCharacterEquipmentResponse from(
            GetCharacterEquipmentResult result
    ) {
        return new GetCharacterEquipmentResponse(
                result.date(),
                result.characterGender(),
                result.characterClass(),
                result.presetNo(),
                convert(result.itemEquipment()),
                convert(result.itemEquipmentPreset1()),
                convert(result.itemEquipmentPreset2()),
                convert(result.itemEquipmentPreset3())
        );
    }

    private static List<ItemEquipmentResponse> convert(
            List<GetCharacterEquipmentResult.ItemEquipmentResult> items
    ) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(item -> ItemEquipmentResponse.from(item))
                .toList();
    }

    public record ItemEquipmentResponse(
            String itemEquipmentPart,
            String itemEquipmentSlot,
            String itemName,
            String itemIcon,
            String itemDescription,
            String itemShapeName,
            String itemShapeIcon,
            String itemGender,
            ItemOptionResponse itemTotalOption,
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
            ItemOptionResponse itemAddOption,
            ItemExceptionalOptionResponse itemExceptionalOption,
            AdditionalOptionEvaluationResponse additionalOptionEvaluation,
            ItemOptionResponse itemEtcOption,
            ItemOptionResponse itemStarforceOption,
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

        public static ItemEquipmentResponse from(
                GetCharacterEquipmentResult.ItemEquipmentResult item
        ) {
            return new ItemEquipmentResponse(
                    item.itemEquipmentPart(),
                    item.itemEquipmentSlot(),
                    item.itemName(),
                    item.itemIcon(),
                    item.itemDescription(),
                    item.itemShapeName(),
                    item.itemShapeIcon(),
                    item.itemGender(),
                    ItemOptionResponse.from(item.itemTotalOption()),
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
                    ItemOptionResponse.from(item.itemAddOption()),
                    ItemExceptionalOptionResponse.from(item.itemExceptionalOption()),
                    AdditionalOptionEvaluationResponse.from(
                            item.additionalOptionEvaluation()
                    ),
                    ItemOptionResponse.from(item.itemEtcOption()),
                    ItemOptionResponse.from(item.itemStarforceOption()),
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

    public record ItemExceptionalOptionResponse(
            String str,
            String dex,
            String intelligence,
            String luk,
            String maxHp,
            String maxMp,
            String attackPower,
            String magicPower,
            Integer exceptionalUpgrade
    ) {

        public static ItemExceptionalOptionResponse from(
                GetCharacterEquipmentResult.ItemExceptionalOptionResult option
        ) {
            if (option == null) {
                return null;
            }

            return new ItemExceptionalOptionResponse(
                    option.str(),
                    option.dex(),
                    option.intelligence(),
                    option.luk(),
                    option.maxHp(),
                    option.maxMp(),
                    option.attackPower(),
                    option.magicPower(),
                    option.exceptionalUpgrade()
            );
        }
    }

    public record ItemOptionResponse(
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

        public static ItemOptionResponse from(
                GetCharacterEquipmentResult.ItemOptionResult option
        ) {
            if (option == null) {
                return null;
            }

            return new ItemOptionResponse(
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
