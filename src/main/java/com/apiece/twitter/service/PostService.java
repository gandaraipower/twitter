package com.apiece.twitter.service;

import com.apiece.twitter.dto.PostRequest;
import com.apiece.twitter.dto.PostResponse;
import com.apiece.twitter.entity.Post;
import com.apiece.twitter.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    // 전체 게시글을 최신순으로 조회
    public List<PostResponse> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PostResponse::from)
                .toList();
    }

    // ID로 게시글 단건 조회
    public PostResponse getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
        return PostResponse.from(post);
    }

    // 새 게시글 작성
    @Transactional
    public PostResponse createPost(PostRequest request) {
        Post post = request.toEntity();
        Post savedPost = postRepository.save(post);
        return PostResponse.from(savedPost);
    }

    // 게시글 내용 수정
    @Transactional
    public PostResponse updatePost(Long id, PostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
        post.updateContent(request.content());
        return PostResponse.from(post);
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
        postRepository.delete(post);
    }
}
