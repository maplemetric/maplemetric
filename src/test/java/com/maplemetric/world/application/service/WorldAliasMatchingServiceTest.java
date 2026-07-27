package com.maplemetric.world.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.maplemetric.world.application.port.out.LoadWorldAliasPort;
import com.maplemetric.world.application.port.out.LoadWorldAliasPort.MatchedWorld;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorldAliasMatchingServiceTest {

    private static final UUID WORLD_ID = UUID.randomUUID();

    @Mock
    private LoadWorldAliasPort loadWorldAliasPort;

    @Test
    void 등록된Alias이름이면Canonical월드를반환한다() {
        WorldAliasMatchingService service = createService();

        given(loadWorldAliasPort.findActiveByNormalizedAliasName("루나"))
                .willReturn(Optional.of(new MatchedWorld(WORLD_ID, "루나")));

        Optional<MatchedWorld> matched = service.matchWorld("루나");

        assertThat(matched).isPresent();
        assertThat(matched.get().worldId()).isEqualTo(WORLD_ID);
        assertThat(matched.get().worldName()).isEqualTo("루나");
    }

    @Test
    void 앞뒤공백과대문자를정규화해조회한다() {
        WorldAliasMatchingService service = createService();

        given(loadWorldAliasPort.findActiveByNormalizedAliasName("luna"))
                .willReturn(Optional.of(new MatchedWorld(WORLD_ID, "루나")));

        Optional<MatchedWorld> matched = service.matchWorld("  LUNA  ");

        assertThat(matched).isPresent();
        verify(loadWorldAliasPort).findActiveByNormalizedAliasName("luna");
    }

    @Test
    void 등록되지않은이름이면빈결과를반환한다() {
        WorldAliasMatchingService service = createService();

        given(loadWorldAliasPort.findActiveByNormalizedAliasName("없는월드"))
                .willReturn(Optional.empty());

        assertThat(service.matchWorld("없는월드")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void 비어있는이름은조회하지않고빈결과를반환한다(String aliasName) {
        WorldAliasMatchingService service = createService();

        assertThat(service.matchWorld(aliasName)).isEmpty();

        verify(loadWorldAliasPort, never())
                .findActiveByNormalizedAliasName(any());
    }

    @Test
    void null이름은조회하지않고빈결과를반환한다() {
        WorldAliasMatchingService service = createService();

        assertThat(service.matchWorld(null)).isEmpty();

        verify(loadWorldAliasPort, never())
                .findActiveByNormalizedAliasName(any());
    }

    @Test
    void Alias조회Port에만의존하고외부API를호출하지않는다() {
        assertThat(
                WorldAliasMatchingService.class.getDeclaredConstructors()
        ).hasSize(1);

        assertThat(
                WorldAliasMatchingService.class
                        .getDeclaredConstructors()[0]
                        .getParameterTypes()
        ).containsExactly(LoadWorldAliasPort.class);
    }

    private WorldAliasMatchingService createService() {
        return new WorldAliasMatchingService(loadWorldAliasPort);
    }
}
