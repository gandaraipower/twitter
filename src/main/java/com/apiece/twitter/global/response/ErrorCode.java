package com.apiece.twitter.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 도메인별 발생할 Error Code 담당 enum
 */
@Getter
public enum ErrorCode {

    // 인증 (A)
    INVALID_TOKEN("A001", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN("A002", HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    UNSUPPORTED_TOKEN("A003", HttpStatus.UNAUTHORIZED, "지원하지 않는 토큰 형식입니다."),
    EMPTY_TOKEN("A004", HttpStatus.UNAUTHORIZED, "토큰이 비어있습니다."),
    UNAUTHORIZED("A005", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    // 사용자 (U)
    DUPLICATE_EMAIL("U001", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND("U002", HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    INVALID_PASSWORD("U003", HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),

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
