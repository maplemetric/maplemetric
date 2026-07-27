package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.maplemetric.ranking.application.port.out.LoadJobAliasPort;
import com.maplemetric.ranking.application.port.out.LoadJobAliasPort.MatchedJob;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class JobAliasMatchingServiceTest {

    private static final UUID JOB_ID = UUID.randomUUID();

    @Mock
    private LoadJobAliasPort loadJobAliasPort;

    @Test
    void 등록된Alias이름이면Canonical직업을반환한다() {
        JobAliasMatchingService service = createService();

        given(loadJobAliasPort.findActiveByNormalizedAliasName("팬텀"))
                .willReturn(Optional.of(new MatchedJob(JOB_ID, "팬텀")));

        Optional<MatchedJob> matched = service.matchJob("팬텀");

        assertThat(matched).isPresent();
        assertThat(matched.get().jobId()).isEqualTo(JOB_ID);
        assertThat(matched.get().jobName()).isEqualTo("팬텀");
    }

    @Test
    void 앞뒤공백과대문자를정규화해조회한다() {
        JobAliasMatchingService service = createService();

        given(loadJobAliasPort.findActiveByNormalizedAliasName("hero"))
                .willReturn(Optional.of(new MatchedJob(JOB_ID, "히어로")));

        Optional<MatchedJob> matched = service.matchJob("  HERO  ");

        assertThat(matched).isPresent();
        verify(loadJobAliasPort).findActiveByNormalizedAliasName("hero");
    }

    @Test
    void 등록되지않은이름이면빈결과와함께WARN로그를남긴다(
            CapturedOutput output
    ) {
        JobAliasMatchingService service = createService();

        given(loadJobAliasPort.findActiveByNormalizedAliasName("없는직업"))
                .willReturn(Optional.empty());

        assertThat(service.matchJob("없는직업")).isEmpty();

        assertThat(output)
                .contains("등록되지 않은 직업 이름입니다.")
                .contains("없는직업");
    }

    @Test
    void 정상매칭시미매칭WARN로그를남기지않는다(CapturedOutput output) {
        JobAliasMatchingService service = createService();

        given(loadJobAliasPort.findActiveByNormalizedAliasName("팬텀"))
                .willReturn(Optional.of(new MatchedJob(JOB_ID, "팬텀")));

        assertThat(service.matchJob("팬텀")).isPresent();

        assertThat(output).doesNotContain("등록되지 않은 직업 이름입니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void 비어있는이름은조회하지않고WARN로그를남긴다(
            String aliasName,
            CapturedOutput output
    ) {
        JobAliasMatchingService service = createService();

        assertThat(service.matchJob(aliasName)).isEmpty();

        verify(loadJobAliasPort, never())
                .findActiveByNormalizedAliasName(any());
        assertThat(output)
                .contains("직업 Alias 매칭 대상 이름이 비어 있습니다.");
    }

    @Test
    void null이름은조회하지않고WARN로그를남긴다(CapturedOutput output) {
        JobAliasMatchingService service = createService();

        assertThat(service.matchJob(null)).isEmpty();

        verify(loadJobAliasPort, never())
                .findActiveByNormalizedAliasName(any());
        assertThat(output)
                .contains("직업 Alias 매칭 대상 이름이 비어 있습니다.");
    }

    @Test
    void Alias조회Port에만의존하고외부API를호출하지않는다() {
        assertThat(
                JobAliasMatchingService.class.getDeclaredConstructors()
        ).hasSize(1);

        assertThat(
                JobAliasMatchingService.class
                        .getDeclaredConstructors()[0]
                        .getParameterTypes()
        ).containsExactly(LoadJobAliasPort.class);
    }

    private JobAliasMatchingService createService() {
        return new JobAliasMatchingService(loadJobAliasPort);
    }
}
