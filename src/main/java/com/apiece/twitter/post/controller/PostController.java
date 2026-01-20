package com.apiece.twitter.post.controller;

import com.apiece.twitter.post.dto.PostRequest;
import com.apiece.twitter.post.dto.PostResponse;
import com.apiece.twitter.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 게시글 전체 조회 API (페이징)
    @GetMapping("/api/posts")
    public ResponseEntity<Page<PostResponse>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.status(HttpStatus.OK).body(postService.getAllPosts(pageable));
    }

    // 게시글 단건 조회 API
    @GetMapping("/api/posts/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(postService.getPost(id));
    }

    // 게시글 작성 API
    @PostMapping("/api/posts")
    public ResponseEntity<PostResponse> createPost(@RequestBody PostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(request));
    }

    // 게시글 수정 API
    @PutMapping("/api/posts/{id}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long id, @RequestBody PostRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(postService.updatePost(id, request));
    }

    // 게시글 삭제 API
    @DeleteMapping("/api/posts/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
