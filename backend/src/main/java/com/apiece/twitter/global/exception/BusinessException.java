package com.apiece.twitter.global.exception;

import com.apiece.twitter.global.response.ErrorCode;
import lombok.Getter;

/**
 * 비즈니스 로직에서 발생하는 예외의 기본 클래스
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
