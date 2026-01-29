package com.apiece.twitter.post.controller;

import com.apiece.twitter.global.response.ApiResponse;
import com.apiece.twitter.post.dto.PostRequest;
import com.apiece.twitter.post.dto.PostResponse;
import com.apiece.twitter.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Post", description = "게시글 API")
public class PostController {

    private final PostService postService;

    // 게시글 전체 조회 API (페이징)
    @GetMapping("/api/posts")
    @Operation(summary = "게시글 전체 조회", description = "페이징을 적용하여 게시글 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getAllPosts(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(postService.getAllPosts(pageable)));
    }

    // 게시글 단건 조회 API
    @GetMapping("/api/posts/{id}")
    @Operation(summary = "게시글 단건 조회", description = "게시글 ID로 특정 게시글을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"code\": \"404\", \"message\": \"존재하지 않는 게시글입니다.\", \"data\": null}")))
    })
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @Parameter(description = "게시글 ID") @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(postService.getPost(id)));
    }

    // 게시글 작성 API
    @PostMapping("/api/posts")
    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(examples = @ExampleObject(value = "{\"code\": \"400\", \"message\": \"게시글 내용은 1자 이상 280자 이하여야 합니다.\", \"data\": null}")))
    })
    public ResponseEntity<ApiResponse<PostResponse>> createPost(@RequestBody PostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(postService.createPost(request)));
    }

    // 게시글 수정 API
    @PutMapping("/api/posts/{id}")
    @Operation(summary = "게시글 수정", description = "기존 게시글을 수정합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"code\": \"404\", \"message\": \"존재하지 않는 게시글입니다.\", \"data\": null}")))
    })
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @Parameter(description = "게시글 ID") @PathVariable Long id,
            @RequestBody PostRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(postService.updatePost(id, request)));
    }

    // 게시글 삭제 API
    @DeleteMapping("/api/posts/{id}")
    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"code\": \"404\", \"message\": \"존재하지 않는 게시글입니다.\", \"data\": null}")))
    })
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @Parameter(description = "게시글 ID") @PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success());
    }
}
