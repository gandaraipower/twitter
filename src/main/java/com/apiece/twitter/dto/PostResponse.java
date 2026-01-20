package com.apiece.twitter.dto;

import com.apiece.twitter.entity.Post;

import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        String content,
        String author,
        LocalDateTime createdAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getContent(),
                post.getAuthor(),
                post.getCreatedAt()
        );
    }
}
