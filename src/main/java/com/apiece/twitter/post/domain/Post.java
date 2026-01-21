package com.apiece.twitter.post.domain;

import com.apiece.twitter.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 280)
    private String content;

    @Column(nullable = false)
    private String author;

    @Builder
    public Post(String content, String author) {
        this.content = content;
        this.author = author;
    }

    // 게시글 내용 수정
    public void updateContent(String content) {
        this.content = content;
    }
}
