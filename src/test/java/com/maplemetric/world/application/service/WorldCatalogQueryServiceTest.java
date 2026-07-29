package com.maplemetric.world.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.maplemetric.world.api.CanonicalWorld;
import com.maplemetric.world.application.port.out.LoadWorldCatalogPort;
import com.maplemetric.world.application.port.out.LoadWorldCatalogPort.AliasMatch;
import com.maplemetric.world.application.port.out.LoadWorldCatalogPort.CanonicalWorldRow;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorldCatalogQueryServiceTest {

    private static final CanonicalWorldRow LUNA = new CanonicalWorldRow(
            "luna",
            "루나",
            CanonicalWorld.Status.ACTIVE,
            1
    );

    private static final CanonicalWorldRow CLOSED_WORLD =
            new CanonicalWorldRow(
                    "closed-world",
                    "종료월드",
                    CanonicalWorld.Status.CLOSED,
                    2
            );

    @Mock
    private LoadWorldCatalogPort loadWorldCatalogPort;

    @Captor
    private ArgumentCaptor<Collection<String>> normalizedNamesCaptor;

    @Test
    void Slug로Canonical월드를조회한다() {
        WorldCatalogQueryService service = createService();

        given(loadWorldCatalogPort.findBySlug("luna"))
                .willReturn(Optional.of(LUNA));

        Optional<CanonicalWorld> found = service.findBySlug("luna");

        assertThat(found).isPresent();
        assertThat(found.get().worldSlug()).isEqualTo("luna");
        assertThat(found.get().worldName()).isEqualTo("루나");
        assertThat(found.get().status())
                .isEqualTo(CanonicalWorld.Status.ACTIVE);
        assertThat(found.get().displayOrder()).isEqualTo(1);
    }

    @Test
    void 종료된월드도Slug로조회한다() {
        WorldCatalogQueryService service = createService();

        given(loadWorldCatalogPort.findBySlug("closed-world"))
                .willReturn(Optional.of(CLOSED_WORLD));

        assertThat(service.findBySlug("closed-world"))
                .isPresent()
                .get()
                .extracting(world -> world.status())
                .isEqualTo(CanonicalWorld.Status.CLOSED);
    }

    @Test
    void 존재하지않는Slug는빈결과를반환한다() {
        WorldCatalogQueryService service = createService();

        given(loadWorldCatalogPort.findBySlug("없는월드"))
                .willReturn(Optional.empty());

        assertThat(service.findBySlug("없는월드")).isEmpty();
    }

    @Test
    void Slug앞뒤공백을제거해조회한다() {
        WorldCatalogQueryService service = createService();

        given(loadWorldCatalogPort.findBySlug("luna"))
                .willReturn(Optional.of(LUNA));

        assertThat(service.findBySlug("  luna  ")).isPresent();

        verify(loadWorldCatalogPort).findBySlug("luna");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void 비어있는Slug는조회하지않는다(String worldSlug) {
        WorldCatalogQueryService service = createService();

        assertThat(service.findBySlug(worldSlug)).isEmpty();

        verify(loadWorldCatalogPort, never()).findBySlug(any());
    }

    @Test
    void nullSlug는조회하지않는다() {
        WorldCatalogQueryService service = createService();

        assertThat(service.findBySlug(null)).isEmpty();

        verify(loadWorldCatalogPort, never()).findBySlug(any());
    }

    @Test
    void 전체Canonical월드를표시순서대로반환한다() {
        WorldCatalogQueryService service = createService();

        given(loadWorldCatalogPort.findAllOrderByDisplayOrder())
                .willReturn(List.of(LUNA, CLOSED_WORLD));

        assertThat(service.findAll())
                .extracting(world -> world.worldSlug())
                .containsExactly("luna", "closed-world");
    }

    @Test
    void 원본이름을Canonical월드로일괄해석한다() {
        WorldCatalogQueryService service = createService();

        given(loadWorldCatalogPort.findActiveByNormalizedAliasNames(any()))
                .willReturn(List.of(
                        new AliasMatch("루나", LUNA),
                        new AliasMatch("종료월드", CLOSED_WORLD)
                ));

        Map<String, CanonicalWorld> resolved =
                service.resolveAliases(List.of("루나", "종료월드"));

        assertThat(resolved).hasSize(2);
        assertThat(resolved.get("루나").worldSlug()).isEqualTo("luna");
        assertThat(resolved.get("종료월드").worldSlug())
                .isEqualTo("closed-world");
    }

    @Test
    void 여러원본이름이같은Canonical월드에매칭되면모두같은값을가진다() {
        WorldCatalogQueryService service = createService();

        given(loadWorldCatalogPort.findActiveByNormalizedAliasNames(any()))
                .willReturn(List.of(
                        new AliasMatch("루나", LUNA),
                        new AliasMatch("luna", LUNA)
                ));

        Map<String, CanonicalWorld> resolved =
                service.resolveAliases(List.of("루나", "LUNA"));

        assertThat(resolved).hasSize(2);
        assertThat(resolved.get("루나")).isEqualTo(resolved.get("LUNA"));
        assertThat(resolved.get("LUNA").worldSlug()).isEqualTo("luna");
    }

    @Test
    void 미매칭이름은결과에서제외해호출자가차이로구분한다() {
        WorldCatalogQueryService service = createService();

        given(loadWorldCatalogPort.findActiveByNormalizedAliasNames(any()))
                .willReturn(List.of(new AliasMatch("루나", LUNA)));

        Map<String, CanonicalWorld> resolved =
                service.resolveAliases(List.of("루나", "없는월드"));

        assertThat(resolved).containsOnlyKeys("루나");
        assertThat(resolved).doesNotContainKey("없는월드");
    }

    @Test
    void 원본이름을정규화해한번만조회한다() {
        WorldCatalogQueryService service = createService();

        given(loadWorldCatalogPort.findActiveByNormalizedAliasNames(any()))
                .willReturn(List.of(new AliasMatch("luna", LUNA)));

        service.resolveAliases(List.of("  Luna  ", "LUNA", "luna"));

        verify(loadWorldCatalogPort)
                .findActiveByNormalizedAliasNames(
                        normalizedNamesCaptor.capture()
                );

        assertThat(Set.copyOf(normalizedNamesCaptor.getValue()))
                .containsExactly("luna");
    }

    @Test
    void 비어있거나null인원본이름은조회대상에서제외한다() {
        WorldCatalogQueryService service = createService();

        given(loadWorldCatalogPort.findActiveByNormalizedAliasNames(any()))
                .willReturn(List.of(new AliasMatch("luna", LUNA)));

        service.resolveAliases(Arrays.asList("luna", null, "", "   "));

        verify(loadWorldCatalogPort)
                .findActiveByNormalizedAliasNames(
                        normalizedNamesCaptor.capture()
                );

        assertThat(normalizedNamesCaptor.getValue())
                .containsExactly("luna");
    }

    @Test
    void 해석대상이없으면조회하지않고빈Map을반환한다() {
        WorldCatalogQueryService service = createService();

        assertThat(service.resolveAliases(List.of())).isEmpty();
        assertThat(service.resolveAliases(null)).isEmpty();
        assertThat(service.resolveAliases(List.of("", "  "))).isEmpty();

        verify(loadWorldCatalogPort, never())
                .findActiveByNormalizedAliasNames(any());
    }

    @Test
    void 해석결과는수정할수없다() {
        WorldCatalogQueryService service = createService();

        given(loadWorldCatalogPort.findActiveByNormalizedAliasNames(any()))
                .willReturn(List.of(new AliasMatch("luna", LUNA)));

        Map<String, CanonicalWorld> resolved =
                service.resolveAliases(List.of("luna"));

        assertThatThrownBy(() -> resolved.put("추가", null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void Catalog조회Port에만의존한다() {
        assertThat(
                WorldCatalogQueryService.class.getDeclaredConstructors()
        ).hasSize(1);

        assertThat(
                WorldCatalogQueryService.class
                        .getDeclaredConstructors()[0]
                        .getParameterTypes()
        ).containsExactly(LoadWorldCatalogPort.class);
    }

    private WorldCatalogQueryService createService() {
        return new WorldCatalogQueryService(loadWorldCatalogPort);
    }
}
