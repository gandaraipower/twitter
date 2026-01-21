package com.apiece.twitter.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 도메인별 발생할 Error Code 담당 enum
 */
@Getter
public enum ErrorCode {

    // 게시글 (P)
    NOT_FOUND_POST("P001", HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),
    INVALID_POST_CONTENT("P002", HttpStatus.BAD_REQUEST, "게시글 내용은 1자 이상 280자 이하여야 합니다."),
    UNAUTHORIZED_POST_ACCESS("P003", HttpStatus.FORBIDDEN, "해당 게시글에 대한 권한이 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
