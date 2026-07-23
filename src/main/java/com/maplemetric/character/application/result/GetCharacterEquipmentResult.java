package com.maplemetric.character.application.result;

import com.maplemetric.character.application.calculator.AdditionalOptionCalculator;
import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort.CharacterEquipment;
import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort.ItemEquipment;
import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort.ItemOption;

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

    public static GetCharacterEquipmentResult from(
            CharacterEquipment equipment,
            String characterClass,
            AdditionalOptionCalculator calculator
    ) {
        return new GetCharacterEquipmentResult(
                equipment.date(),
                equipment.characterGender(),
                equipment.characterClass(),
                equipment.presetNo(),
                convertItems(
                        equipment.itemEquipment(),
                        characterClass,
                        calculator
                ),
                convertItems(
                        equipment.itemEquipmentPreset1(),
                        characterClass,
                        calculator
                ),
                convertItems(
                        equipment.itemEquipmentPreset2(),
                        characterClass,
                        calculator
                ),
                convertItems(
                        equipment.itemEquipmentPreset3(),
                        characterClass,
                        calculator
                )
        );
    }

    private static List<ItemEquipmentResult> convertItems(
            List<ItemEquipment> items,
            String characterClass,
            AdditionalOptionCalculator calculator
    ) {
        if (items == null) {
            return null;
        }

        return items.stream()
                .map(item -> ItemEquipmentResult.from(
                        item,
                        characterClass,
                        calculator
                ))
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
            AdditionalOptionEvaluationResult additionalOptionEvaluation,
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
        public static ItemEquipmentResult from(
                ItemEquipment item,
                String characterClass,
                AdditionalOptionCalculator calculator
        ) {
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
                    calculator.calculate(
                            characterClass,
                            item.itemAddOption()
                    ),
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

        public static ItemOptionResult from(ItemOption option) {
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

