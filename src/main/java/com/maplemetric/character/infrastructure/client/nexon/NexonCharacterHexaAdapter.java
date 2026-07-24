package com.maplemetric.character.infrastructure.client.nexon;

import com.maplemetric.character.application.port.out.LoadCharacterHexaPort;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.CharacterHexa;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.HexaCore;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.HexaStatCore;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.LinkedSkill;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.SixthSkill;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSkillResponse;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
class NexonCharacterHexaAdapter
        implements LoadCharacterHexaPort {

    private static final String SIXTH_SKILL_GRADE = "6";

    private final CharacterClient characterClient;

    NexonCharacterHexaAdapter(
            CharacterClient characterClient
    ) {
        this.characterClient = characterClient;
    }

    @Override
    public CharacterHexa loadCharacterHexa(
            String ocid
    ) {
        CharacterHexaMatrixResponse matrixResponse =
                characterClient.getCharacterHexaMatrix(ocid);

        CharacterHexaMatrixStatResponse statResponse =
                characterClient.getCharacterHexaMatrixStat(ocid);

        CharacterSkillResponse sixthSkillResponse =
                characterClient.getCharacterSkill(
                        ocid,
                        SIXTH_SKILL_GRADE
                );

        return new CharacterHexa(
                convertList(
                        matrixResponse.characterHexaCoreEquipment(),
                        core -> toHexaCore(core)
                ),
                convertList(
                        sixthSkillResponse.characterSkill(),
                        skill -> new SixthSkill(
                                skill.skillName(),
                                skill.skillIcon()
                        )
                ),
                convertList(
                        statResponse.characterHexaStatCore(),
                        stat -> toHexaStatCore(stat)
                ),
                convertList(
                        statResponse.characterHexaStatCore2(),
                        stat -> toHexaStatCore(stat)
                ),
                convertList(
                        statResponse.characterHexaStatCore3(),
                        stat -> toHexaStatCore(stat)
                )
        );
    }

    private HexaCore toHexaCore(
            CharacterHexaMatrixResponse.HexaCore core
    ) {
        return new HexaCore(
                core.hexaCoreName(),
                core.hexaCoreType(),
                core.hexaCoreLevel(),
                convertList(
                        core.linkedSkill(),
                        linkedSkill -> new LinkedSkill(
                                linkedSkill.hexaSkillId()
                        )
                )
        );
    }

    private HexaStatCore toHexaStatCore(
            CharacterHexaMatrixStatResponse.HexaStatCore stat
    ) {
        return new HexaStatCore(
                stat.slotId(),
                stat.mainStatName(),
                stat.mainStatLevel(),
                stat.subStatName1(),
                stat.subStatLevel1(),
                stat.subStatName2(),
                stat.subStatLevel2()
        );
    }

    private <S, T> List<T> convertList(
            List<S> source,
            Function<S, T> mapper
    ) {
        if (source == null) {
            return null;
        }

        return source.stream()
                .map(item -> item == null
                        ? null
                        : mapper.apply(item))
                .toList();
    }
}
