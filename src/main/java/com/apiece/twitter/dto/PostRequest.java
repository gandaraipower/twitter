package com.apiece.twitter.dto;

import com.apiece.twitter.entity.Post;

public record PostRequest(
        String content,
        String author
) {
    public Post toEntity() {
        return Post.builder()
                .content(content)
                .author(author)
                .build();
    }
}
