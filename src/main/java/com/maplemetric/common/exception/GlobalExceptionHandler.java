package com.maplemetric.common.exception;

import com.maplemetric.common.ApiResponse;
import com.maplemetric.common.BusinessException;
import com.maplemetric.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 모든 응답이 {@link ApiResponse} 형식을 지키게 한다.
 *
 * 우선순위를 가장 낮게 둔다. Spring은 Advice를 순회하다 매칭되는 메서드가 있는
 * 첫 Advice를 쓰므로, 순서를 정하지 않으면 여기의 포괄 처리가 모듈별 Advice의
 * 구체적인 처리를 가로챌 수 있다.
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return createErrorResponse(exception.getErrorCode());
    }

    /**
     * 요청을 읽지 못한 경우다.
     *
     * {@code HttpMessageNotReadableException}은 본문 JSON이 깨졌을 때 발생한다.
     * 이것을 서버 오류로 돌려주면 보낸 쪽이 자기 요청을 의심하지 않는다.
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleValidationException() {
        return createErrorResponse(
                GlobalErrorCode.INVALID_INPUT_VALUE
        );
    }

    /**
     * 예상하지 못한 오류다.
     *
     * 이 처리가 없으면 Spring 기본 오류 바디가 나가 {@code success} 필드조차 없다.
     * 소비자가 그 필드로 분기하면 그때 깨진다.
     *
     * {@code Exception}이 아니라 {@code RuntimeException}만 잡는다. 경로를 찾지
     * 못했거나 허용되지 않은 Method 같은 Servlet 예외는 검사 예외이며 Spring이
     * 이미 알맞은 상태 코드로 처리한다. 그것까지 잡으면 404가 500이 된다.
     *
     * 원인은 로그에만 남긴다. 예외 메시지에는 SQL과 제약 이름이 섞여 들어온다.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            RuntimeException exception
    ) {
        log.error(
                "처리하지 못한 예외가 발생했습니다. type={}",
                exception.getClass().getName(),
                exception
        );

        return createErrorResponse(
                GlobalErrorCode.INTERNAL_SERVER_ERROR
        );
    }

    private ResponseEntity<ApiResponse<Void>> createErrorResponse(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }
}
