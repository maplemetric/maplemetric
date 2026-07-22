package com.maplemetric.common.nexon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.global.BusinessException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
public final class NexonApiRequester {

    private static final String RATE_LIMIT_ERROR_CODE =
            "OPENAPI00007";

    private static final int MAX_RATE_LIMIT_RETRY_COUNT = 3;

    private static final long RATE_LIMIT_RETRY_DELAY_MILLIS =
            1_000L;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Function<NexonApiFailure, ? extends BusinessException>
            exceptionFactory;
    private final BiPredicate<String, String> notFoundPredicate;

    public NexonApiRequester(
            RestClient restClient,
            ObjectMapper objectMapper,
            Function<NexonApiFailure, ? extends BusinessException>
                    exceptionFactory,
            BiPredicate<String, String> notFoundPredicate
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.exceptionFactory = exceptionFactory;
        this.notFoundPredicate = notFoundPredicate;
    }

    public <T> T request(
            String path,
            Map<String, String> queryParameters,
            Class<T> responseType,
            String apiName,
            String identifierName,
            String identifierValue
    ) {
        return request(
                path,
                queryParameters,
                responseType,
                apiName,
                identifierName,
                identifierValue,
                0
        );
    }

    private <T> T request(
            String path,
            Map<String, String> queryParameters,
            Class<T> responseType,
            String apiName,
            String identifierName,
            String identifierValue,
            int rateLimitRetryCount
    ) {
        String logIdentifierValue =
                maskIdentifierValue(
                        identifierName,
                        identifierValue
                );

        try {
            T response = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path);

                        queryParameters.forEach(
                                (name, value) ->
                                        uriBuilder.queryParam(
                                                name,
                                                value
                                        )
                        );

                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(responseType);

            if (response == null) {
                log.warn(
                        "넥슨 API 응답 본문이 없습니다. api={}, {}={}",
                        apiName,
                        identifierName,
                        logIdentifierValue
                );

                throw createException(
                        NexonApiFailure.RESPONSE_INVALID
                );
            }

            return response;

        } catch (HttpClientErrorException.NotFound exception) {
            NexonApiErrorResponse.NexonApiError nexonError =
                    getNexonError(
                            apiName,
                            exception
                    );

            log.info(
                    "넥슨 API 조회 결과가 없습니다. "
                            + "api={}, status={}, nexonErrorCode={}, "
                            + "nexonErrorMessage={}, {}={}",
                    apiName,
                    exception.getStatusCode(),
                    getNexonErrorCode(nexonError),
                    getLogNexonErrorMessage(
                            nexonError,
                            identifierName,
                            identifierValue
                    ),
                    identifierName,
                    logIdentifierValue
            );

            throw createException(
                    NexonApiFailure.NOT_FOUND
            );

        } catch (HttpClientErrorException exception) {
            NexonApiErrorResponse.NexonApiError nexonError =
                    getNexonError(
                            apiName,
                            exception
                    );

            if (shouldRetryRateLimit(
                    exception,
                    nexonError,
                    rateLimitRetryCount
            )) {
                long retryDelayMillis =
                        getRateLimitRetryDelayMillis(
                                rateLimitRetryCount
                        );

                log.warn(
                        "넥슨 API 요청 제한 응답으로 재시도합니다. "
                                + "api={}, status={}, nexonErrorCode={}, "
                                + "nexonErrorMessage={}, {}={}, "
                                + "retryCount={}, retryDelayMillis={}",
                        apiName,
                        exception.getStatusCode(),
                        getNexonErrorCode(nexonError),
                        getLogNexonErrorMessage(
                                nexonError,
                                identifierName,
                                identifierValue
                        ),
                        identifierName,
                        logIdentifierValue,
                        rateLimitRetryCount + 1,
                        retryDelayMillis
                );

                sleepBeforeRetry(retryDelayMillis);

                return request(
                        path,
                        queryParameters,
                        responseType,
                        apiName,
                        identifierName,
                        identifierValue,
                        rateLimitRetryCount + 1
                );
            }

            throw convertClientErrorException(
                    exception,
                    apiName,
                    identifierName,
                    identifierValue,
                    nexonError
            );

        } catch (HttpServerErrorException exception) {
            log.error(
                    "넥슨 API 서버 오류가 발생했습니다. "
                            + "api={}, status={}, {}={}, exceptionType={}",
                    apiName,
                    exception.getStatusCode(),
                    identifierName,
                    logIdentifierValue,
                    exception.getClass().getSimpleName()
            );

            throw createException(
                    NexonApiFailure.SERVER_ERROR
            );

        } catch (ResourceAccessException exception) {
            throw convertResourceAccessException(
                    exception,
                    apiName,
                    identifierName,
                    identifierValue
            );

        } catch (RestClientException exception) {
            log.error(
                    "넥슨 API 응답 처리에 실패했습니다. "
                            + "api={}, {}={}, exceptionType={}",
                    apiName,
                    identifierName,
                    logIdentifierValue,
                    exception.getClass().getSimpleName()
            );

            throw createException(
                    NexonApiFailure.RESPONSE_INVALID
            );
        }
    }

    private BusinessException convertClientErrorException(
            HttpClientErrorException exception,
            String apiName,
            String identifierName,
            String identifierValue,
            NexonApiErrorResponse.NexonApiError nexonError
    ) {
        String nexonErrorCode =
                getNexonErrorCode(nexonError);

        String nexonErrorMessage =
                getLogNexonErrorMessage(
                        nexonError,
                        identifierName,
                        identifierValue
                );

        String logIdentifierValue =
                maskIdentifierValue(
                        identifierName,
                        identifierValue
                );

        if (notFoundPredicate.test(
                apiName,
                nexonErrorCode
        )) {
            log.info(
                    "넥슨 API 캐릭터 조회 결과가 없습니다. "
                            + "api={}, status={}, nexonErrorCode={}, "
                            + "nexonErrorMessage={}, {}={}",
                    apiName,
                    exception.getStatusCode(),
                    nexonErrorCode,
                    nexonErrorMessage,
                    identifierName,
                    logIdentifierValue
            );

            return createException(
                    NexonApiFailure.NOT_FOUND
            );
        }

        log.warn(
                "넥슨 API 요청 오류가 발생했습니다. "
                        + "api={}, status={}, nexonErrorCode={}, "
                        + "nexonErrorMessage={}, {}={}",
                apiName,
                exception.getStatusCode(),
                nexonErrorCode,
                nexonErrorMessage,
                identifierName,
                logIdentifierValue
        );

        return createException(
                NexonApiFailure.CLIENT_ERROR
        );
    }

    private boolean shouldRetryRateLimit(
            HttpClientErrorException exception,
            NexonApiErrorResponse.NexonApiError nexonError,
            int rateLimitRetryCount
    ) {
        if (rateLimitRetryCount >= MAX_RATE_LIMIT_RETRY_COUNT) {
            return false;
        }

        return exception.getStatusCode().value() == 429
                && RATE_LIMIT_ERROR_CODE.equals(
                getNexonErrorCode(nexonError)
        );
    }

    private long getRateLimitRetryDelayMillis(
            int rateLimitRetryCount
    ) {
        return RATE_LIMIT_RETRY_DELAY_MILLIS
                * (rateLimitRetryCount + 1);
    }

    private void sleepBeforeRetry(
            long retryDelayMillis
    ) {
        try {
            Thread.sleep(retryDelayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread()
                    .interrupt();

            throw createException(
                    NexonApiFailure.SERVER_ERROR
            );
        }
    }

    private BusinessException convertResourceAccessException(
            ResourceAccessException exception,
            String apiName,
            String identifierName,
            String identifierValue
    ) {
        String logIdentifierValue =
                maskIdentifierValue(
                        identifierName,
                        identifierValue
                );

        if (isTimeout(exception)) {
            log.error(
                    "넥슨 API 응답 시간이 초과되었습니다. "
                            + "api={}, {}={}, exceptionType={}",
                    apiName,
                    identifierName,
                    logIdentifierValue,
                    exception.getClass().getSimpleName()
            );

            return createException(
                    NexonApiFailure.TIMEOUT
            );
        }

        log.error(
                "넥슨 API 통신에 실패했습니다. "
                        + "api={}, {}={}, exceptionType={}",
                apiName,
                identifierName,
                logIdentifierValue,
                exception.getClass().getSimpleName()
        );

        return createException(
                NexonApiFailure.SERVER_ERROR
        );
    }

    private NexonApiErrorResponse.NexonApiError getNexonError(
            String apiName,
            HttpClientErrorException exception
    ) {
        try {
            NexonApiErrorResponse errorResponse =
                    objectMapper.readValue(
                            exception.getResponseBodyAsByteArray(),
                            NexonApiErrorResponse.class
                    );

            if (errorResponse == null
                    || errorResponse.error() == null
                    || !StringUtils.hasText(
                    errorResponse.error().name()
            )) {
                log.warn(
                        "넥슨 API 오류 응답에 오류 코드가 없습니다. "
                                + "api={}, status={}",
                        apiName,
                        exception.getStatusCode()
                );

                return null;
            }

            return errorResponse.error();

        } catch (IOException parsingException) {
            log.warn(
                    "넥슨 API 오류 응답 파싱에 실패했습니다. "
                            + "api={}, status={}, exceptionType={}",
                    apiName,
                    exception.getStatusCode(),
                    parsingException.getClass().getSimpleName()
            );

            return null;
        }
    }

    private String getNexonErrorCode(
            NexonApiErrorResponse.NexonApiError nexonError
    ) {
        return nexonError == null
                ? null
                : nexonError.name();
    }

    private String getNexonErrorMessage(
            NexonApiErrorResponse.NexonApiError nexonError
    ) {
        return nexonError == null
                ? null
                : nexonError.message();
    }

    private String getLogNexonErrorMessage(
            NexonApiErrorResponse.NexonApiError nexonError,
            String identifierName,
            String identifierValue
    ) {
        String nexonErrorMessage =
                getNexonErrorMessage(nexonError);

        if (!"ocid".equals(identifierName)
                || !StringUtils.hasText(identifierValue)
                || !StringUtils.hasText(nexonErrorMessage)) {
            return nexonErrorMessage;
        }

        return nexonErrorMessage.replace(
                identifierValue,
                maskIdentifierValue(
                        identifierName,
                        identifierValue
                )
        );
    }

    private String maskIdentifierValue(
            String identifierName,
            String identifierValue
    ) {
        if (!"ocid".equals(identifierName)
                || !StringUtils.hasText(identifierValue)) {
            return identifierValue;
        }

        if (identifierValue.length() <= 8) {
            return "***";
        }

        return identifierValue.substring(0, 4)
                + "..."
                + identifierValue.substring(
                        identifierValue.length() - 4
                );
    }

    private boolean isTimeout(
            Throwable throwable
    ) {
        Throwable cause = throwable;

        while (cause != null) {
            if (cause instanceof HttpTimeoutException
                    || cause instanceof SocketTimeoutException) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    private BusinessException createException(
            NexonApiFailure failure
    ) {
        return exceptionFactory.apply(failure);
    }
}
