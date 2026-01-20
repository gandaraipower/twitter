---
name: spring-api-rules
description: Spring Boot REST API 개발을 위한 아키텍처 표준 (컨트롤러, 서비스, 엔티티, DTO 등). 팀 협업 규칙 및 주석 가이드 포함.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, LSP
---

# Spring Boot API 개발 표준 가이드

이 프로젝트의 REST API 개발을 위한 범용 규칙입니다.

## 📦 패키지 구조 (도메인형 - 필수!)

**⚠️ 반드시 도메인 완전 분리형 구조를 사용하세요.**

```
com.example.project
├── {domain}/                    # 도메인별 패키지 (예: post, user, order)
│   ├── controller/
│   │   └── {Domain}Controller.java
│   ├── dto/
│   │   ├── {Domain}Request.java
│   │   └── {Domain}Response.java
│   ├── domain/
│   │   └── {Domain}.java        # 엔티티
│   ├── repository/
│   │   └── {Domain}Repository.java
│   └── service/
│       └── {Domain}Service.java
├── common/                      # 공통 모듈 (예외, 유틸 등)
│   └── exception/
└── config/                      # 전역 설정
```

### 예시: Post 도메인
```
com.apiece.twitter
└── post/
    ├── controller/
    │   └── PostController.java
    ├── dto/
    │   ├── PostRequest.java
    │   └── PostResponse.java
    ├── domain/
    │   └── Post.java
    ├── repository/
    │   └── PostRepository.java
    └── service/
        └── PostService.java
```

## 🔑 핵심 공통 규칙

1.  **의존성 주입 (DI):** 항상 `@RequiredArgsConstructor`를 사용한 **생성자 주입**을 사용하세요. 필드 주입(`@Autowired`) 금지.
2.  **불변성:** DTO와 설정 파일은 Java `record`를 사용하세요.
3.  **Lombok:** `@Getter`는 자유롭게 사용하되, 엔티티에 `@Setter` 사용은 지양하고 비즈니스 메서드를 만드세요.
4.  **📝 주석(Documentation):** 모든 Public 메서드(Controller 엔드포인트, Service 로직) 위에는 **한 줄 기능 설명**을 작성해야 합니다.

## 🎮 컨트롤러 (Controller)

-   **위치:** `{domain}/controller/` 패키지
-   `@RestController` 사용.
-   클래스 레벨에 `@RequestMapping` 금지. 메서드에 전체 경로 명시.
-   반환 타입은 항상 `ResponseEntity<T>` 사용.
-   네이밍: `{Domain}Controller`
-   **✅ 주석 필수:** 각 엔드포인트 메서드 바로 위에 API 기능을 설명하는 한 줄 주석을 작성하세요.

### ResponseEntity 상태 코드 (중요!)
-   **⚠️ 반드시 `ResponseEntity.status(HttpStatus.XXX).body()` 형식 사용**
-   `.ok()` 같은 축약형 금지

| HTTP Method | 상태 코드 | 사용법 |
|-------------|-----------|--------|
| GET | `HttpStatus.OK` (200) | `ResponseEntity.status(HttpStatus.OK).body(data)` |
| POST | `HttpStatus.CREATED` (201) | `ResponseEntity.status(HttpStatus.CREATED).body(data)` |
| PUT/PATCH | `HttpStatus.OK` (200) | `ResponseEntity.status(HttpStatus.OK).body(data)` |
| DELETE | `HttpStatus.NO_CONTENT` (204) | `ResponseEntity.status(HttpStatus.NO_CONTENT).build()` |

### 컨트롤러 예시
```java
package com.apiece.twitter.post.controller;

// 게시글 전체 조회 API (페이징)
@GetMapping("/api/posts")
public ResponseEntity<Page<PostResponse>> getAllPosts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    return ResponseEntity.status(HttpStatus.OK).body(postService.getAllPosts(pageable));
}

// 게시글 작성 API
@PostMapping("/api/posts")
public ResponseEntity<PostResponse> createPost(@RequestBody PostRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(request));
}

// 게시글 삭제 API
@DeleteMapping("/api/posts/{id}")
public ResponseEntity<Void> deletePost(@PathVariable Long id) {
    postService.deletePost(id);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
}
```

## 📨 DTO 전략 (중요!)

### 필수 규칙
-   **위치:** `{domain}/dto/` 패키지
-   **⚠️ 반드시 Java `record` 사용 (class 금지!)**
-   비즈니스 로직 포함 금지.

### Request DTO
-   **⚠️ `toEntity()` 메서드 필수 구현**
-   Service에서 `request.toEntity()` 형태로 사용
    ```java
    package com.apiece.twitter.post.dto;

    public record PostRequest(
            String content,
            String author
    ) {
        public Post toEntity() {
            return Post.builder()
                    .content(content)
                    .author(author)
                    .build();
        }
    }
    ```

### Response DTO
-   **⚠️ `static from(Entity)` 팩토리 메서드 필수 구현**
-   Service에서 `PostResponse.from(entity)` 또는 `.map(PostResponse::from)` 형태로 사용
    ```java
    package com.apiece.twitter.post.dto;

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
    ```

### Service에서 DTO 사용 예시
```java
// 조회 - PostResponse.from() 사용
public Page<PostResponse> getAllPosts(Pageable pageable) {
    return postRepository.findAll(pageable)
            .map(PostResponse::from);  // new PostResponse() 금지!
}

// 생성 - request.toEntity() 사용
@Transactional
public PostResponse createPost(PostRequest request) {
    Post post = request.toEntity();  // 직접 Builder 호출 금지!
    Post savedPost = postRepository.save(post);
    return PostResponse.from(savedPost);
}
```

### 주석 가이드
-   **✅ 주석 권장:** 필드명이 명확하지 않거나 제약조건(예: 필수값, 길이제한)이 있는 경우 필드 위에 주석을 작성하세요.

## 🏢 도메인 계층 (Domain)

### 1. 엔티티 (Entity)
-   **위치:** `{domain}/domain/` 패키지
-   `protected` 기본 생성자 필수.
-   `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
-   모든 연관관계는 `FetchType.LAZY`.
-   `@JoinColumn` 사용 (물리적 FK 제약은 상황에 따라 제외 가능).

### 2. 리포지토리 (Repository)
-   **위치:** `{domain}/repository/` 패키지
-   `JpaRepository<Entity, ID>` 확장.

### 3. 서비스 (Service)
-   **위치:** `{domain}/service/` 패키지
-   **쓰기(Create, Update, Delete):** `@Transactional` 필수.
-   **읽기(Read):** 단순 조회는 트랜잭션 불필요. 복잡한 조회만 `@Transactional(readOnly = true)`.
-   도메인별 예외(`{Domain}Exception`)를 만들고 `@RestControllerAdvice`로 처리.
-   **✅ 주석 필수:** 주요 비즈니스 로직 메서드 위에 **기능 요약 주석**을 작성하세요.
    ```java
    // 게시글 작성 및 포인트 적립 처리
    @Transactional
    public PostResponse createPost(...) { ... }
    ```

## 🛡️ 협업 및 작업 범위 규칙 (중요!)

이 프로젝트는 **팀 프로젝트**이므로 다음 규칙을 엄격히 준수하세요:

1.  **범위 확인:** 작업 시작 전, 사용자가 어떤 도메인(기능)을 작업 중인지 파악하세요.
2.  **격리 (Isolation):** 사용자의 작업 도메인 외부 패키지는 **절대 수정하지 마세요.**
    -   (예: `post/` 작업 중이면 `user/` 패키지 건드리지 말 것)
3.  **동의 구하기:** `common/` 모듈이나 다른 사람의 도메인을 수정해야 한다면 반드시 먼저 물어보세요.

## 🧪 테스트 스크립트

API 개발 시 `src/main/resources/http/` 경로에 curl 스크립트 생성을 권장합니다.
-   파일명: `{resource}.sh` (예: `posts.sh`)
