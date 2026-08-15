package com.maplemetric.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maplemetric.common.BusinessException;
import com.maplemetric.common.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전역 예외 처리의 경계를 고정한다.
 *
 * 실제 Endpoint 대신 이 Test 전용 Controller를 쓴다. 실제 Endpoint에 기대면
 * 그 Endpoint가 바뀔 때 전역 처리와 무관한 이유로 이 Test가 깨진다.
 */
@WebMvcTest(GlobalExceptionHandlerTest.FixtureController.class)
@Import(GlobalExceptionHandlerTest.FixtureController.class)
class GlobalExceptionHandlerTest {

    private static final String FIXTURE_PATH = "/test/global-exception";

    @Autowired
    private MockMvc mockMvc;

    /**
     * 허용되지 않은 Method가 500이 되지 않는지 본다.
     *
     * 전역 처리가 RuntimeException이 아니라 Exception을 잡으면 Spring이 이미
     * 405로 처리한 것을 가로채 500으로 바꾼다. 그 회귀를 여기서 막는다.
     *
     * 응답 본문은 확인하지 않는다. MockMvc는 실제 Container의 error page
     * 재요청을 재현하지 않아 라우팅 오류의 본문 형식을 신뢰할 수 없다.
     */
    @Test
    void 허용되지않은Method는405로남는다() throws Exception {
        mockMvc.perform(post(FIXTURE_PATH))
                .andExpect(status().isMethodNotAllowed());
    }

    /**
     * 등록되지 않은 경로가 500이 되지 않는지 본다.
     *
     * 같은 이유로 상태 코드만 확인한다.
     */
    @Test
    void 등록되지않은경로는404로남는다() throws Exception {
        mockMvc.perform(get(FIXTURE_PATH + "/등록되지-않은-하위-경로"))
                .andExpect(status().isNotFound());
    }

    /**
     * BusinessException은 자신의 ErrorCode를 그대로 내보낸다.
     *
     * 모듈이 던지는 예외가 공통 500으로 뭉개지지 않아야 소비자가 code로
     * 분기할 수 있다.
     */
    @Test
    void BusinessException은자체오류코드로응답한다() throws Exception {
        mockMvc.perform(get(FIXTURE_PATH + "/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FIXTURE_001"))
                .andExpect(jsonPath("$.message").value("고정 오류입니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @RestController
    @RequestMapping(FIXTURE_PATH)
    static class FixtureController {

        @GetMapping
        String ok() {
            return "ok";
        }

        @GetMapping("/business")
        String business() {
            throw new FixtureException();
        }
    }

    private static class FixtureException extends BusinessException {

        FixtureException() {
            super(FixtureErrorCode.FIXTURE_CONFLICT);
        }
    }

    private enum FixtureErrorCode implements ErrorCode {

        FIXTURE_CONFLICT(
                HttpStatus.CONFLICT,
                "FIXTURE_001",
                "고정 오류입니다."
        );

        private final HttpStatus httpStatus;
        private final String code;
        private final String message;

        FixtureErrorCode(
                HttpStatus httpStatus,
                String code,
                String message
        ) {
            this.httpStatus = httpStatus;
            this.code = code;
            this.message = message;
        }

        @Override
        public HttpStatus getHttpStatus() {
            return httpStatus;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
