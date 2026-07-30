package com.maplemetric.character.infrastructure.client.nexon;

import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort;
import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort.CharacterSetEffect;
import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort.SetEffect;
import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort.SetOption;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterSetEffectResponse;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
class NexonCharacterSetEffectAdapter
        implements LoadCharacterSetEffectPort {

    private final CharacterClient characterClient;

    NexonCharacterSetEffectAdapter(
            CharacterClient characterClient
    ) {
        this.characterClient = characterClient;
    }

    @Override
    public CharacterSetEffect loadCharacterSetEffect(
            String ocid
    ) {
        CharacterSetEffectResponse response =
                characterClient.getCharacterSetEffect(ocid);

        return new CharacterSetEffect(
                convertList(
                        response.setEffect(),
                        setEffect -> toSetEffect(setEffect)
                )
        );
    }

    private SetEffect toSetEffect(
            CharacterSetEffectResponse.SetEffect setEffect
    ) {
        return new SetEffect(
                setEffect.setName(),
                setEffect.totalSetCount(),
                convertList(
                        setEffect.setEffectInfo(),
                        option -> toSetOption(option)
                ),
                convertList(
                        setEffect.setOptionFull(),
                        option -> toSetOption(option)
                )
        );
    }

    private SetOption toSetOption(
            CharacterSetEffectResponse.SetEffectInfo option
    ) {
        return new SetOption(
                option.setCount(),
                option.setOption()
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
