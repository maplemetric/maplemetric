package com.maplemetric.character.infrastructure.client.nexon;

import com.maplemetric.character.application.port.out.LoadCharacterAbilityPort;
import com.maplemetric.character.application.port.out.LoadCharacterAbilityPort.AbilityOption;
import com.maplemetric.character.application.port.out.LoadCharacterAbilityPort.AbilityPreset;
import com.maplemetric.character.application.port.out.LoadCharacterAbilityPort.CharacterAbility;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterAbilityResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class NexonCharacterAbilityAdapter
        implements LoadCharacterAbilityPort {

    private final CharacterClient characterClient;

    NexonCharacterAbilityAdapter(
            CharacterClient characterClient
    ) {
        this.characterClient = characterClient;
    }

    @Override
    public CharacterAbility loadCharacterAbility(
            String ocid
    ) {
        CharacterAbilityResponse response =
                characterClient.getCharacterAbility(ocid);

        return new CharacterAbility(
                response.date(),
                response.abilityGrade(),
                convertOptions(response.abilityInfo()),
                response.remainFame(),
                response.presetNo(),
                convertPreset(response.abilityPreset1()),
                convertPreset(response.abilityPreset2()),
                convertPreset(response.abilityPreset3())
        );
    }

    private AbilityPreset convertPreset(
            CharacterAbilityResponse.AbilityPreset preset
    ) {
        if (preset == null) {
            return null;
        }

        return new AbilityPreset(
                preset.abilityPresetGrade(),
                convertOptions(preset.abilityInfo())
        );
    }

    private List<AbilityOption> convertOptions(
            List<CharacterAbilityResponse.AbilityInfo> options
    ) {
        if (options == null) {
            return null;
        }

        return options.stream()
                .map(option -> option == null
                        ? null
                        : new AbilityOption(
                                option.abilityNo(),
                                option.abilityGrade(),
                                option.abilityValue()
                        ))
                .toList();
    }
}
