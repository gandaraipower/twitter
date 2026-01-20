package com.apiece.twitter.post.repository;

import com.apiece.twitter.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
