package com.maplemetric.character.infrastructure.client.nexon;

import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort;
import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort.CharacterEquipment;
import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort.ItemEquipment;
import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort.ItemBaseOption;
import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort.ItemExceptionalOption;
import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort.ItemOption;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterEquipmentResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class NexonCharacterEquipmentAdapter
        implements LoadCharacterEquipmentPort {

    private final CharacterClient characterClient;

    NexonCharacterEquipmentAdapter(
            CharacterClient characterClient
    ) {
        this.characterClient = characterClient;
    }

    @Override
    public CharacterEquipment loadCharacterEquipment(
            String ocid
    ) {
        CharacterEquipmentResponse response =
                characterClient.getCharacterEquipment(ocid);

        return new CharacterEquipment(
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

    private List<ItemEquipment> convertItems(
            List<CharacterEquipmentResponse.ItemEquipment> items
    ) {
        if (items == null) {
            return null;
        }

        return items.stream()
                .map(item -> convertItem(item))
                .toList();
    }

    private ItemEquipment convertItem(
            CharacterEquipmentResponse.ItemEquipment item
    ) {
        return new ItemEquipment(
                item.itemEquipmentPart(),
                item.itemEquipmentSlot(),
                item.itemName(),
                item.itemIcon(),
                item.itemDescription(),
                item.itemShapeName(),
                item.itemShapeIcon(),
                item.itemGender(),
                convertOption(item.itemTotalOption()),
                convertBaseOption(item.itemBaseOption()),
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
                convertOption(item.itemAddOption()),
                convertExceptionalOption(item.itemExceptionalOption()),
                convertOption(item.itemEtcOption()),
                convertOption(item.itemStarforceOption()),
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

    private ItemBaseOption convertBaseOption(
            CharacterEquipmentResponse.ItemBaseOption option
    ) {
        if (option == null) {
            return null;
        }

        return new ItemBaseOption(
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
                option.maxHpRate(),
                option.maxMpRate(),
                option.baseEquipmentLevel()
        );
    }

    private ItemExceptionalOption convertExceptionalOption(
            CharacterEquipmentResponse.ItemExceptionalOption option
    ) {
        if (option == null) {
            return null;
        }

        return new ItemExceptionalOption(
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

    private ItemOption convertOption(
            CharacterEquipmentResponse.ItemOption option
    ) {
        if (option == null) {
            return null;
        }

        return new ItemOption(
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
