package com.apiece.twitter.post.controller;

import com.apiece.twitter.post.dto.PostRequest;
import com.apiece.twitter.post.dto.PostResponse;
import com.apiece.twitter.post.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PostController 테스트")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    @Test
    @DisplayName("GET /api/posts - 게시글 전체 조회")
    void getAllPosts() throws Exception {
        // given
        List<PostResponse> posts = List.of(
                createPostResponse(1L, "게시글 1", "작성자1"),
                createPostResponse(2L, "게시글 2", "작성자2")
        );
        Page<PostResponse> postPage = new PageImpl<>(posts);
        given(postService.getAllPosts(any(Pageable.class))).willReturn(postPage);

        // when & then
        mockMvc.perform(get("/api/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].content").value("게시글 1"));
    }

    @Test
    @DisplayName("GET /api/posts/{id} - 게시글 단건 조회")
    void getPost() throws Exception {
        // given
        Long postId = 1L;
        PostResponse response = createPostResponse(postId, "테스트 게시글", "홍길동");
        given(postService.getPost(postId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/posts/{id}", postId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.id").value(postId))
                .andExpect(jsonPath("$.data.content").value("테스트 게시글"));
    }

    @Test
    @DisplayName("POST /api/posts - 게시글 작성")
    void createPost() throws Exception {
        // given
        PostRequest request = new PostRequest("새 게시글", "홍길동");
        PostResponse response = createPostResponse(1L, "새 게시글", "홍길동");
        given(postService.createPost(any(PostRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.content").value("새 게시글"));
    }

    @Test
    @DisplayName("PUT /api/posts/{id} - 게시글 수정")
    void updatePost() throws Exception {
        // given
        Long postId = 1L;
        PostRequest request = new PostRequest("수정된 내용", "홍길동");
        PostResponse response = createPostResponse(postId, "수정된 내용", "홍길동");
        given(postService.updatePost(eq(postId), any(PostRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(put("/api/posts/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.content").value("수정된 내용"));
    }

    @Test
    @DisplayName("DELETE /api/posts/{id} - 게시글 삭제")
    void deletePost() throws Exception {
        // given
        Long postId = 1L;
        doNothing().when(postService).deletePost(postId);

        // when & then
        mockMvc.perform(delete("/api/posts/{id}", postId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    // 테스트용 PostResponse 생성 헬퍼 메서드
    private PostResponse createPostResponse(Long id, String content, String author) {
        return new PostResponse(id, content, author, LocalDateTime.now(), LocalDateTime.now());
    }
}
