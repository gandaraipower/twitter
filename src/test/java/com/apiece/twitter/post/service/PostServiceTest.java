package com.apiece.twitter.post.service;

import com.apiece.twitter.post.domain.Post;
import com.apiece.twitter.post.dto.PostRequest;
import com.apiece.twitter.post.dto.PostResponse;
import com.apiece.twitter.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService 테스트")
class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Test
    @DisplayName("게시글 전체 조회 - 페이징")
    void getAllPosts() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> posts = List.of(
                createPost(1L, "게시글 1", "작성자1"),
                createPost(2L, "게시글 2", "작성자2")
        );
        Page<Post> postPage = new PageImpl<>(posts, pageable, posts.size());
        given(postRepository.findAll(pageable)).willReturn(postPage);

        // when
        Page<PostResponse> result = postService.getAllPosts(pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).content()).isEqualTo("게시글 1");
    }

    @Test
    @DisplayName("게시글 단건 조회 - 성공")
    void getPost_Success() {
        // given
        Long postId = 1L;
        Post post = createPost(postId, "테스트 게시글", "홍길동");
        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when
        PostResponse result = postService.getPost(postId);

        // then
        assertThat(result.id()).isEqualTo(postId);
        assertThat(result.content()).isEqualTo("테스트 게시글");
    }

    @Test
    @DisplayName("게시글 단건 조회 - 존재하지 않는 게시글")
    void getPost_NotFound() {
        // given
        Long postId = 999L;
        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.getPost(postId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");
    }

    @Test
    @DisplayName("게시글 작성")
    void createPost() {
        // given
        PostRequest request = new PostRequest("새 게시글", "홍길동");
        Post savedPost = createPost(1L, "새 게시글", "홍길동");
        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        PostResponse result = postService.createPost(request);

        // then
        assertThat(result.content()).isEqualTo("새 게시글");
        assertThat(result.author()).isEqualTo("홍길동");
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 수정")
    void updatePost() {
        // given
        Long postId = 1L;
        Post existingPost = createPost(postId, "원래 내용", "홍길동");
        PostRequest request = new PostRequest("수정된 내용", "홍길동");
        given(postRepository.findById(postId)).willReturn(Optional.of(existingPost));

        // when
        PostResponse result = postService.updatePost(postId, request);

        // then
        assertThat(result.content()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("게시글 삭제")
    void deletePost() {
        // given
        Long postId = 1L;
        Post post = createPost(postId, "삭제할 게시글", "홍길동");
        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when
        postService.deletePost(postId);

        // then
        verify(postRepository).delete(post);
    }

    // 테스트용 Post 엔티티 생성 헬퍼 메서드
    private Post createPost(Long id, String content, String author) {
        Post post = Post.builder()
                .content(content)
                .author(author)
                .build();
        // Reflection을 사용하여 ID 설정 (테스트용)
        try {
            java.lang.reflect.Field idField = Post.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(post, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return post;
    }
}
