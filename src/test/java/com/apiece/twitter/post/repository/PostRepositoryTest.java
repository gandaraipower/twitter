package com.apiece.twitter.post.repository;

import com.apiece.twitter.post.domain.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("PostRepository 테스트")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("게시글 저장")
    void save() {
        // given
        Post post = Post.builder()
                .content("테스트 게시글")
                .author("홍길동")
                .build();

        // when
        Post savedPost = postRepository.save(post);

        // then
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getContent()).isEqualTo("테스트 게시글");
        assertThat(savedPost.getAuthor()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("게시글 ID로 조회")
    void findById() {
        // given
        Post post = postRepository.save(Post.builder()
                .content("테스트 게시글")
                .author("홍길동")
                .build());

        // when
        Optional<Post> foundPost = postRepository.findById(post.getId());

        // then
        assertThat(foundPost).isPresent();
        assertThat(foundPost.get().getContent()).isEqualTo("테스트 게시글");
    }

    @Test
    @DisplayName("게시글 페이징 조회")
    void findAllWithPaging() {
        // given
        for (int i = 1; i <= 15; i++) {
            postRepository.save(Post.builder()
                    .content("게시글 " + i)
                    .author("작성자" + i)
                    .build());
        }
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("id").descending());

        // when
        Page<Post> postPage = postRepository.findAll(pageRequest);

        // then
        assertThat(postPage.getContent()).hasSize(10);
        assertThat(postPage.getTotalElements()).isEqualTo(15);
        assertThat(postPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("게시글 삭제")
    void delete() {
        // given
        Post post = postRepository.save(Post.builder()
                .content("삭제할 게시글")
                .author("홍길동")
                .build());
        Long postId = post.getId();

        // when
        postRepository.delete(post);

        // then
        Optional<Post> deletedPost = postRepository.findById(postId);
        assertThat(deletedPost).isEmpty();
    }
}
