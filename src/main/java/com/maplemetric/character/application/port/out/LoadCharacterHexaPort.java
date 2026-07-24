package com.maplemetric.character.application.port.out;

import java.util.List;

public interface LoadCharacterHexaPort {

    CharacterHexa loadCharacterHexa(String ocid);

    record CharacterHexa(
            List<HexaCore> hexaCoreEquipment,
            List<SixthSkill> sixthSkills,
            List<HexaStatCore> hexaStatCore1,
            List<HexaStatCore> hexaStatCore2,
            List<HexaStatCore> hexaStatCore3
    ) {
    }

    record HexaCore(
            String hexaCoreName,
            String hexaCoreType,
            Integer hexaCoreLevel,
            List<LinkedSkill> linkedSkills
    ) {
    }

    record LinkedSkill(
            String hexaSkillId
    ) {
    }

    record SixthSkill(
            String skillName,
            String skillIcon
    ) {
    }

    record HexaStatCore(
            String slotId,
            String mainStatName,
            Integer mainStatLevel,
            String subStatName1,
            Integer subStatLevel1,
            String subStatName2,
            Integer subStatLevel2
    ) {
    }
}
