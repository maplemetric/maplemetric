package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.maplemetric.ranking.api.CanonicalJob;
import com.maplemetric.ranking.application.port.out.LoadJobCatalogPort;
import com.maplemetric.ranking.application.port.out.LoadJobCatalogPort.AliasMatch;
import com.maplemetric.ranking.application.port.out.LoadJobCatalogPort.CanonicalJobRow;
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
class JobCatalogQueryServiceTest {

    private static final CanonicalJobRow HERO = new CanonicalJobRow(
            "hero",
            "히어로",
            "모험가",
            "전사",
            true,
            1
    );

    private static final CanonicalJobRow BISHOP = new CanonicalJobRow(
            "bishop",
            "비숍",
            "모험가",
            "마법사",
            true,
            2
    );

    @Mock
    private LoadJobCatalogPort loadJobCatalogPort;

    @Captor
    private ArgumentCaptor<Collection<String>> normalizedNamesCaptor;

    @Test
    void Slug로Canonical직업을조회한다() {
        JobCatalogQueryService service = createService();

        given(loadJobCatalogPort.findBySlug("hero"))
                .willReturn(Optional.of(HERO));

        Optional<CanonicalJob> found = service.findBySlug("hero");

        assertThat(found).isPresent();
        assertThat(found.get().jobSlug()).isEqualTo("hero");
        assertThat(found.get().jobName()).isEqualTo("히어로");
        assertThat(found.get().jobGroup()).isEqualTo("모험가");
        assertThat(found.get().jobBranch()).isEqualTo("전사");
        assertThat(found.get().available()).isTrue();
        assertThat(found.get().displayOrder()).isEqualTo(1);
    }

    @Test
    void 존재하지않는Slug는빈결과를반환한다() {
        JobCatalogQueryService service = createService();

        given(loadJobCatalogPort.findBySlug("없는직업"))
                .willReturn(Optional.empty());

        assertThat(service.findBySlug("없는직업")).isEmpty();
    }

    @Test
    void Slug앞뒤공백을제거해조회한다() {
        JobCatalogQueryService service = createService();

        given(loadJobCatalogPort.findBySlug("hero"))
                .willReturn(Optional.of(HERO));

        assertThat(service.findBySlug("  hero  ")).isPresent();

        verify(loadJobCatalogPort).findBySlug("hero");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void 비어있는Slug는조회하지않는다(String jobSlug) {
        JobCatalogQueryService service = createService();

        assertThat(service.findBySlug(jobSlug)).isEmpty();

        verify(loadJobCatalogPort, never()).findBySlug(any());
    }

    @Test
    void nullSlug는조회하지않는다() {
        JobCatalogQueryService service = createService();

        assertThat(service.findBySlug(null)).isEmpty();

        verify(loadJobCatalogPort, never()).findBySlug(any());
    }

    @Test
    void 전체Canonical직업을표시순서대로반환한다() {
        JobCatalogQueryService service = createService();

        given(loadJobCatalogPort.findAllOrderByDisplayOrder())
                .willReturn(List.of(HERO, BISHOP));

        assertThat(service.findAll())
                .extracting(job -> job.jobSlug())
                .containsExactly("hero", "bishop");
    }

    @Test
    void 원본이름을Canonical직업으로일괄해석한다() {
        JobCatalogQueryService service = createService();

        given(loadJobCatalogPort.findActiveByNormalizedAliasNames(any()))
                .willReturn(List.of(
                        new AliasMatch("히어로", HERO),
                        new AliasMatch("비숍", BISHOP)
                ));

        Map<String, CanonicalJob> resolved =
                service.resolveAliases(List.of("히어로", "비숍"));

        assertThat(resolved).hasSize(2);
        assertThat(resolved.get("히어로").jobSlug()).isEqualTo("hero");
        assertThat(resolved.get("비숍").jobSlug()).isEqualTo("bishop");
    }

    @Test
    void 여러원본이름이같은Canonical직업에매칭되면모두같은값을가진다() {
        JobCatalogQueryService service = createService();

        given(loadJobCatalogPort.findActiveByNormalizedAliasNames(any()))
                .willReturn(List.of(
                        new AliasMatch("히어로", HERO),
                        new AliasMatch("hero", HERO)
                ));

        Map<String, CanonicalJob> resolved =
                service.resolveAliases(List.of("히어로", "HERO"));

        assertThat(resolved).hasSize(2);
        assertThat(resolved.get("히어로")).isEqualTo(resolved.get("HERO"));
        assertThat(resolved.get("HERO").jobSlug()).isEqualTo("hero");
    }

    @Test
    void 미매칭이름은결과에서제외해호출자가차이로구분한다() {
        JobCatalogQueryService service = createService();

        given(loadJobCatalogPort.findActiveByNormalizedAliasNames(any()))
                .willReturn(List.of(new AliasMatch("히어로", HERO)));

        Map<String, CanonicalJob> resolved =
                service.resolveAliases(List.of("히어로", "없는직업"));

        assertThat(resolved).containsOnlyKeys("히어로");
        assertThat(resolved).doesNotContainKey("없는직업");
    }

    @Test
    void 원본이름을정규화해한번만조회한다() {
        JobCatalogQueryService service = createService();

        given(loadJobCatalogPort.findActiveByNormalizedAliasNames(any()))
                .willReturn(List.of(new AliasMatch("hero", HERO)));

        service.resolveAliases(List.of("  Hero  ", "HERO", "hero"));

        verify(loadJobCatalogPort)
                .findActiveByNormalizedAliasNames(
                        normalizedNamesCaptor.capture()
                );

        assertThat(Set.copyOf(normalizedNamesCaptor.getValue()))
                .containsExactly("hero");
    }

    @Test
    void 비어있거나null인원본이름은조회대상에서제외한다() {
        JobCatalogQueryService service = createService();

        given(loadJobCatalogPort.findActiveByNormalizedAliasNames(any()))
                .willReturn(List.of(new AliasMatch("hero", HERO)));

        service.resolveAliases(Arrays.asList("hero", null, "", "   "));

        verify(loadJobCatalogPort)
                .findActiveByNormalizedAliasNames(
                        normalizedNamesCaptor.capture()
                );

        assertThat(normalizedNamesCaptor.getValue())
                .containsExactly("hero");
    }

    @Test
    void 해석대상이없으면조회하지않고빈Map을반환한다() {
        JobCatalogQueryService service = createService();

        assertThat(service.resolveAliases(List.of())).isEmpty();
        assertThat(service.resolveAliases(null)).isEmpty();
        assertThat(service.resolveAliases(List.of("", "  "))).isEmpty();

        verify(loadJobCatalogPort, never())
                .findActiveByNormalizedAliasNames(any());
    }

    @Test
    void 해석결과는수정할수없다() {
        JobCatalogQueryService service = createService();

        given(loadJobCatalogPort.findActiveByNormalizedAliasNames(any()))
                .willReturn(List.of(new AliasMatch("hero", HERO)));

        Map<String, CanonicalJob> resolved =
                service.resolveAliases(List.of("hero"));

        assertThatThrownBy(() -> resolved.put("추가", null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void Catalog조회Port에만의존한다() {
        assertThat(
                JobCatalogQueryService.class.getDeclaredConstructors()
        ).hasSize(1);

        assertThat(
                JobCatalogQueryService.class
                        .getDeclaredConstructors()[0]
                        .getParameterTypes()
        ).containsExactly(LoadJobCatalogPort.class);
    }

    private JobCatalogQueryService createService() {
        return new JobCatalogQueryService(loadJobCatalogPort);
    }
}
