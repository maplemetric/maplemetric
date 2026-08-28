package com.maplemetric.common.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.common.nexon.NexonRateLimitProperties;
import com.maplemetric.common.nexon.NexonRequestRateGate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

/**
 * 공유 Nexon 요청 계층의 분류와 마스킹 경계를 고정한다.
 *
 * 이 클래스는 네 Client가 함께 쓴다. 여기서 실패 분류가 바뀌면 어느 Client에서
 * 깨질지 예측하기 어려우므로 Client를 거치지 않고 직접 검증한다.
 *
 * 정상 응답·5xx·timeout·재시도 성공·재시도 소진처럼 이미 Client Test가 고정한
 * 동작은 여기서 다시 만들지 않는다. 재시도가 일어나는 경로는 최소 1초를 대기하는데
 * 아래 경계는 모두 재시도가 일어나지 않아 추가 지연이 없다.
 */
@ExtendWith(OutputCaptureExtension.class)
class NexonApiRequesterTest {

    private static final String BASE_URL = "https://open.api.nexon.com";
    private static final String PATH = "/maplestory/v1/id";
    private static final String API_NAME = "테스트API";

    private static final String OCID =
            "1234567890abcdefghijklmnopqrstuv";

    private static final String MASKED_OCID = "1234...stuv";

    private static final String RATE_LIMIT_CODE = "OPENAPI00007";

    /** 어떤 코드도 미조회로 보지 않는다. */
    private static final BiPredicate<String, String> NEVER_NOT_FOUND =
            (apiName, errorCode) -> false;

    private MockRestServiceServer mockServer;

    private NexonApiRequester createRequester(
            BiPredicate<String, String> notFoundPredicate
    ) {
        return createRequester(
                notFoundPredicate,
                new NexonRequestRateGate(
                        new NexonRateLimitProperties(1000, java.time.Duration.ofMinutes(1)),
                        java.util.List.of("test-key")
                )
        );
    }

    private NexonApiRequester createRequester(
            BiPredicate<String, String> notFoundPredicate,
            NexonRequestRateGate rateGate
    ) {
        RestClient.Builder builder =
                RestClient.builder().baseUrl(BASE_URL);

        mockServer = MockRestServiceServer.bindTo(builder).build();

        return new NexonApiRequester(
                builder.build(),
                new ObjectMapper(),
                TestNexonException::new,
                notFoundPredicate,
                rateGate
        );
    }

    private String request(
            NexonApiRequester requester,
            String identifierName,
            String identifierValue
    ) {
        return requester.request(
                PATH,
                Map.of(),
                String.class,
                API_NAME,
                identifierName,
                identifierValue
        );
    }

    private void expectOnce(ResponseCreator responseCreator) {
        mockServer.expect(requestTo(BASE_URL + PATH))
                .andRespond(responseCreator);
    }

    private ResponseCreator errorResponse(
            HttpStatus status,
            String body
    ) {
        return withStatus(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private String errorBody(String code, String message) {
        return """
                {
                  "error": {
                    "name": "%s",
                    "message": "%s"
                  }
                }
                """.formatted(code, message);
    }

    private TestNexonException requestExpectingFailure(
            NexonApiRequester requester,
            String identifierName,
            String identifierValue
    ) {
        return catchThrowableOfType(
                () -> request(
                        requester,
                        identifierName,
                        identifierValue
                ),
                TestNexonException.class
        );
    }

    /**
     * 404는 Nexon이 조회 결과 없음을 알리는 방식이라 코드 판정을 거치지 않는다.
     */
    @Test
    void 사공사는코드판정과무관하게미조회다() {
        NexonApiRequester requester = createRequester(NEVER_NOT_FOUND);

        expectOnce(
                errorResponse(
                        HttpStatus.NOT_FOUND,
                        errorBody("OPENAPI00004", "조회 결과가 없습니다.")
                )
        );

        TestNexonException exception =
                requestExpectingFailure(requester, "ocid", OCID);

        assertThat(exception.failure())
                .isEqualTo(NexonApiFailure.NOT_FOUND);

        mockServer.verify();
    }

    /**
     * 404가 아니어도 Client가 미조회로 보기로 한 코드는 미조회다.
     */
    @Test
    void 미조회코드로지정하면사공사가아니어도미조회다() {
        NexonApiRequester requester = createRequester(
                (apiName, errorCode) ->
                        "OPENAPI00004".equals(errorCode)
        );

        expectOnce(
                errorResponse(
                        HttpStatus.BAD_REQUEST,
                        errorBody("OPENAPI00004", "식별자가 올바르지 않습니다.")
                )
        );

        TestNexonException exception =
                requestExpectingFailure(requester, "ocid", OCID);

        assertThat(exception.failure())
                .isEqualTo(NexonApiFailure.NOT_FOUND);

        mockServer.verify();
    }

    /**
     * 재시도 조건은 상태 코드와 오류 코드의 AND다.
     *
     * 상태 코드만 맞다고 재시도하면 요청 제한이 아닌 429까지 외부 호출을 늘린다.
     * 요청이 한 번만 나가는지로 확인한다.
     */
    @Test
    void 사이구라도요청제한코드가아니면재시도하지않는다() {
        NexonApiRequester requester = createRequester(NEVER_NOT_FOUND);

        expectOnce(
                errorResponse(
                        HttpStatus.TOO_MANY_REQUESTS,
                        errorBody("OPENAPI00009", "요청 제한이 아닙니다.")
                )
        );

        TestNexonException exception =
                requestExpectingFailure(requester, "ocid", OCID);

        assertThat(exception.failure())
                .isEqualTo(NexonApiFailure.CLIENT_ERROR);

        // 재시도가 일어났다면 두 번째 요청에서 검증이 실패한다.
        mockServer.verify();
    }

    /**
     * 반대 방향도 마찬가지다. 오류 코드만 맞다고 재시도하지 않는다.
     */
    @Test
    void 요청제한코드라도사이구가아니면재시도하지않는다() {
        NexonApiRequester requester = createRequester(NEVER_NOT_FOUND);

        expectOnce(
                errorResponse(
                        HttpStatus.BAD_REQUEST,
                        errorBody(RATE_LIMIT_CODE, "요청이 올바르지 않습니다.")
                )
        );

        TestNexonException exception =
                requestExpectingFailure(requester, "ocid", OCID);

        assertThat(exception.failure())
                .isEqualTo(NexonApiFailure.CLIENT_ERROR);

        mockServer.verify();
    }

    /**
     * 재시도를 다 써도 한도 초과라는 사실이 남는다.
     *
     * 일반 Client 오류로 뭉개면 요청 자체가 잘못된 것과 구별되지 않는다. 그러면
     * 부르는 쪽이 영구 실패로 판정해, 한도가 풀려도 다시 시도하지 않는다.
     * 실제로 그 이유로 Backfill 기준일 63개가 영구 실패로 닫혔다.
     */
    @Test
    void 재시도를다써도한도초과라는사실이남는다() {
        NexonApiRequester requester = createRequester(NEVER_NOT_FOUND);

        // 첫 요청과 재시도 세 번 모두 한도 초과다.
        for (int attempt = 0; attempt < 4; attempt++) {
            expectOnce(
                    errorResponse(
                            HttpStatus.TOO_MANY_REQUESTS,
                            errorBody(RATE_LIMIT_CODE, "요청이 많습니다.")
                    )
            );
        }

        TestNexonException exception =
                requestExpectingFailure(requester, "ocid", OCID);

        assertThat(exception.failure())
                .isEqualTo(NexonApiFailure.RATE_LIMITED);

        mockServer.verify();
    }

    /**
     * 오류 응답이 깨져 있어도 파싱 실패가 밖으로 새지 않는다.
     *
     * 외부가 보낸 본문 때문에 분류 자체가 중단되면 소비자는 원인을 알 수 없는
     * 예외를 받는다.
     */
    @Test
    void 오류응답이깨져있어도분류를계속한다() {
        NexonApiRequester requester = createRequester(NEVER_NOT_FOUND);

        expectOnce(
                errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "{ this is not json"
                )
        );

        TestNexonException exception =
                requestExpectingFailure(requester, "ocid", OCID);

        assertThat(exception.failure())
                .isEqualTo(NexonApiFailure.CLIENT_ERROR);

        mockServer.verify();
    }

    /**
     * 오류 코드가 없으면 코드 없이 판정한다.
     */
    @Test
    void 오류코드가없으면코드없이판정한다() {
        NexonApiRequester requester = createRequester(
                (apiName, errorCode) -> errorCode != null
        );

        expectOnce(
                errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "{ \"error\": null }"
                )
        );

        TestNexonException exception =
                requestExpectingFailure(requester, "ocid", OCID);

        // predicate가 코드 있음을 요구하므로 미조회가 아니라 요청 오류다.
        assertThat(exception.failure())
                .isEqualTo(NexonApiFailure.CLIENT_ERROR);

        mockServer.verify();
    }

    /**
     * 짧은 식별자는 앞뒤를 남기면 값이 거의 그대로 드러난다.
     */
    @Test
    void 식별자가짧으면값전체를가린다(CapturedOutput output) {
        NexonApiRequester requester = createRequester(NEVER_NOT_FOUND);

        String shortOcid = "12345678";

        expectOnce(
                errorResponse(
                        HttpStatus.BAD_REQUEST,
                        errorBody("OPENAPI00004", "잘못된 요청입니다.")
                )
        );

        requestExpectingFailure(requester, "ocid", shortOcid);

        assertThat(output).contains("***");
        assertThat(output).doesNotContain(shortOcid);
    }

    /**
     * 마스킹은 ocid에만 적용한다.
     *
     * 캐릭터명이나 월드명까지 가리면 운영 로그에서 대상을 특정할 수 없다.
     */
    @Test
    void ocid가아닌식별자는가리지않는다(CapturedOutput output) {
        NexonApiRequester requester = createRequester(NEVER_NOT_FOUND);

        String characterName = "테스트캐릭터명";

        expectOnce(
                errorResponse(
                        HttpStatus.BAD_REQUEST,
                        errorBody("OPENAPI00004", "잘못된 요청입니다.")
                )
        );

        requestExpectingFailure(
                requester,
                "characterName",
                characterName
        );

        assertThat(output).contains(characterName);
    }

    /**
     * Nexon이 오류 메시지 안에 식별자를 되돌려주는 경우가 있다.
     *
     * 식별자 항목만 가리고 메시지를 그대로 남기면 같은 값이 로그에 남는다.
     */
    @Test
    void 오류메시지안의식별자도가린다(CapturedOutput output) {
        NexonApiRequester requester = createRequester(NEVER_NOT_FOUND);

        expectOnce(
                errorResponse(
                        HttpStatus.BAD_REQUEST,
                        errorBody(
                                "OPENAPI00004",
                                "ocid " + OCID + " 는 올바르지 않습니다."
                        )
                )
        );

        requestExpectingFailure(requester, "ocid", OCID);

        assertThat(output).doesNotContain(OCID);
        assertThat(output).contains(MASKED_OCID);
    }

    /**
     * 정상 응답은 그대로 돌려준다.
     *
     * 실패 분류만 모아두면 성공 경로가 함께 깨져도 이 클래스가 알려주지 못한다.
     */
    @Test
    void 정상응답은본문을그대로반환한다() {
        NexonApiRequester requester = createRequester(NEVER_NOT_FOUND);

        expectOnce(
                withSuccess(
                        "응답",
                        MediaType.APPLICATION_JSON
                )
        );

        String result = request(requester, "ocid", OCID);

        assertThat(result).isEqualTo("응답");

        mockServer.verify();
    }

    /**
     * 요청 하나마다 관문에서 허가를 받는다.
     *
     * 관문을 만들어 두고 요청 경로에서 부르지 않으면 초당 한도는 그대로 넘긴다.
     * 연결 자체를 고정한다.
     */
    @Test
    void 요청마다관문에서허가를받는다() {
        CountingRateGate gate = new CountingRateGate();

        NexonApiRequester requester = createRequester(NEVER_NOT_FOUND, gate);

        expectOnce(withSuccess("응답", MediaType.APPLICATION_JSON));

        request(requester, "ocid", OCID);

        assertThat(gate.count()).isEqualTo(1);

        mockServer.verify();
    }

    /**
     * 재시도도 관문을 다시 지난다.
     *
     * 요청 제한을 맞고 되돌아온 요청이 관문을 건너뛰면, 한도를 넘긴 상태에서
     * 다시 나가 상황을 악화시킨다.
     */
    @Test
    void 재시도도관문을다시지난다() {
        CountingRateGate gate = new CountingRateGate();

        NexonApiRequester requester = createRequester(NEVER_NOT_FOUND, gate);

        // 첫 요청은 요청 제한, 두 번째는 성공이다.
        expectOnce(
                errorResponse(
                        HttpStatus.TOO_MANY_REQUESTS,
                        errorBody(RATE_LIMIT_CODE, "요청이 많습니다.")
                )
        );
        expectOnce(withSuccess("응답", MediaType.APPLICATION_JSON));

        request(requester, "ocid", OCID);

        assertThat(gate.count()).isEqualTo(2);

        mockServer.verify();
    }

    /**
     * 허가를 기다리다 중단되면 재시도 대기와 같은 실패로 알린다.
     *
     * 여기서만 다른 예외를 던지면 전역 처리기가 500으로 바꾼다. 같은 계층의 같은
     * 상황인데 소비자가 받는 응답이 달라진다.
     */
    @Test
    void 허가를기다리다중단되면서버오류로알린다() {
        NexonApiRequester requester = createRequester(
                NEVER_NOT_FOUND,
                new InterruptingRateGate()
        );

        TestNexonException exception =
                requestExpectingFailure(requester, "ocid", OCID);

        assertThat(exception.failure())
                .isEqualTo(NexonApiFailure.SERVER_ERROR);

        // 중단 표시를 삼키지 않는다.
        assertThat(Thread.interrupted()).isTrue();
    }

    /** 허가를 기다리다 중단된 상황을 만든다. */
    private static final class InterruptingRateGate
            extends NexonRequestRateGate {

        private InterruptingRateGate() {
            super(new NexonRateLimitProperties(1000, java.time.Duration.ofMinutes(1)), java.util.List.of("test-key"));
        }

        @Override
        public String acquire() throws InterruptedException {
            throw new InterruptedException("허가 대기 중단");
        }
    }

    /** 허가 요청 횟수만 센다. 실제로 기다리지 않는다. */
    private static final class CountingRateGate extends NexonRequestRateGate {

        private final AtomicInteger count = new AtomicInteger();

        private CountingRateGate() {
            super(new NexonRateLimitProperties(1000, java.time.Duration.ofMinutes(1)), java.util.List.of("test-key"));
        }

        @Override
        public String acquire() {
            count.incrementAndGet();
            
            return "test-key";
        }

        private int count() {
            return count.get();
        }
    }

    /** Client마다 다른 예외 타입을 대신하는 테스트 전용 예외다. */
    private static final class TestNexonException extends RuntimeException {

        private final transient NexonApiFailure failure;

        private TestNexonException(NexonApiFailure failure) {
            this.failure = failure;
        }

        private NexonApiFailure failure() {
            return failure;
        }
    }
}
