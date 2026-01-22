package com.apiece.twitter.post.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Post 엔티티 테스트")
class PostTest {

    @Test
    @DisplayName("Post 엔티티 생성 - Builder 패턴")
    void createPost() {
        // given
        String content = "테스트 게시글 내용입니다.";
        String author = "홍길동";

        // when
        Post post = Post.builder()
                .content(content)
                .author(author)
                .build();

        // then
        assertThat(post.getContent()).isEqualTo(content);
        assertThat(post.getAuthor()).isEqualTo(author);
    }

    @Test
    @DisplayName("Post 내용 수정")
    void updateContent() {
        // given
        Post post = Post.builder()
                .content("원래 내용")
                .author("홍길동")
                .build();
        String newContent = "수정된 내용";

        // when
        post.updateContent(newContent);

        // then
        assertThat(post.getContent()).isEqualTo(newContent);
    }
}
