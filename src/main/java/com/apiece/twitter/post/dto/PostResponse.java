package com.apiece.twitter.post.dto;

import com.apiece.twitter.post.domain.Post;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "게시글 응답")
public record PostResponse(
        @Schema(description = "게시글 ID", example = "1")
        Long id,

        @Schema(description = "게시글 내용", example = "오늘 날씨가 좋네요!")
        String content,

        @Schema(description = "작성자", example = "홍길동")
        String author,

        @Schema(description = "생성 시간", example = "2024-01-15T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 시간", example = "2024-01-15T11:00:00")
        LocalDateTime modifiedAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getContent(),
                post.getAuthor(),
                post.getCreatedAt(),
                post.getModifiedAt()
        );
    }
}
