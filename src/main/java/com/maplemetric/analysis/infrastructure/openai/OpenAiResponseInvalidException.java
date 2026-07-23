package com.maplemetric.analysis.infrastructure.openai;

final class OpenAiResponseInvalidException extends RuntimeException {

    OpenAiResponseInvalidException() {
        super("OpenAI 응답을 인사이트로 사용할 수 없습니다.");
    }
}
