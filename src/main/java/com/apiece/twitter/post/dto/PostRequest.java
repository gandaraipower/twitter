package com.apiece.twitter.post.dto;

import com.apiece.twitter.post.domain.Post;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 작성/수정 요청")
public record PostRequest(
        @Schema(description = "게시글 내용", example = "오늘 날씨가 좋네요!", maxLength = 280)
        String content,

        @Schema(description = "작성자", example = "홍길동")
        String author
) {
    public Post toEntity() {
        return Post.builder()
                .content(content)
                .author(author)
                .build();
    }
}
