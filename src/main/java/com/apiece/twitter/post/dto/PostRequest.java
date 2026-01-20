package com.apiece.twitter.post.dto;

import com.apiece.twitter.post.domain.Post;

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
