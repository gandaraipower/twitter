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
├── global/                      # 전역 공통 모듈
│   ├── config/
│   │   └── SpringDoc.java       # Swagger/OpenAPI 설정
│   ├── exception/
│   │   ├── BusinessException.java      # 비즈니스 예외 기본 클래스
│   │   └── GlobalExceptionHandler.java # 전역 예외 처리 핸들러
│   ├── jpa/
│   │   └── entity/
│   │       └── BaseEntity.java  # 공통 엔티티 (createDate, modifyDate)
│   ├── response/
│   │   ├── ApiResponse.java     # 공통 응답 래퍼
│   │   ├── ErrorCode.java       # 도메인별 에러 코드
│   │   └── ResponseCode.java    # 공통 응답 코드
│   └── security/                # 인증/인가 모듈
│       ├── config/
│       │   └── SecurityConfig.java
│       ├── jwt/
│       │   ├── JwtTokenProvider.java
│       │   ├── JwtAuthenticationFilter.java
│       │   └── JwtProperties.java
│       ├── oauth2/              # 소셜 로그인 (선택)
│       │   ├── CustomOAuth2UserService.java
│       │   ├── OAuth2SuccessHandler.java
│       │   └── userinfo/
│       └── handler/
│           ├── CustomAuthenticationEntryPoint.java
│           └── CustomAccessDeniedHandler.java
├── {domain}/                    # 도메인별 패키지 (예: post, user, order)
│   ├── controller/
│   │   └── {Domain}Controller.java
│   ├── dto/
│   │   ├── {Domain}Request.java
│   │   └── {Domain}Response.java
│   ├── domain/
│   │   └── {Domain}.java        # 엔티티 (BaseEntity 상속 필수)
│   ├── repository/
│   │   └── {Domain}Repository.java
│   └── service/
│       └── {Domain}Service.java
└── {Project}Application.java    # @EnableJpaAuditing 필수
```

### 예시: Post 도메인
```
com.apiece.twitter
├── global/
│   ├── config/
│   │   └── SpringDoc.java
│   ├── exception/
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   ├── jpa/entity/
│   │   └── BaseEntity.java
│   └── response/
│       ├── ApiResponse.java
│       ├── ErrorCode.java
│       └── ResponseCode.java
├── post/
│   ├── controller/
│   │   └── PostController.java
│   ├── dto/
│   │   ├── PostRequest.java
│   │   └── PostResponse.java
│   ├── domain/
│   │   └── Post.java
│   ├── repository/
│   │   └── PostRepository.java
│   └── service/
│       └── PostService.java
└── TwitterApplication.java
```

## 🔑 핵심 공통 규칙

1.  **의존성 주입 (DI):** 항상 `@RequiredArgsConstructor`를 사용한 **생성자 주입**을 사용하세요. 필드 주입(`@Autowired`) 금지.
2.  **불변성:** DTO와 설정 파일은 Java `record`를 사용하세요.
3.  **Lombok:** `@Getter`는 자유롭게 사용하되, 엔티티에 `@Setter` 사용은 지양하고 비즈니스 메서드를 만드세요.
4.  **📝 주석(Documentation):** 모든 Public 메서드(Controller 엔드포인트, Service 로직) 위에는 **한 줄 기능 설명**을 작성해야 합니다.

## 🌐 Global 모듈 (전역 공통)

### 1. BaseEntity (JPA Auditing)
-   **위치:** `global/jpa/entity/BaseEntity.java`
-   **⚠️ 모든 엔티티는 반드시 `BaseEntity`를 상속**하여 `createdAt`, `modifiedAt` 자동 관리
-   **⚠️ Application 클래스에 `@EnableJpaAuditing` 필수**
-   **⚠️ `@Column`으로 DB 컬럼명 명시 필수** (Java 필드명 ↔ DB 컬럼명 불일치 방지)

```java
@MappedSuperclass
@Getter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}

// 사용 예시
@Entity
public class Post extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // ... (createdAt, modifiedAt은 자동 관리됨)
}
```

**⚠️ 주의: Java 필드명 vs DB 컬럼명**
| Java 필드명 | DB 컬럼명 | 설명 |
|-------------|-----------|------|
| `createdAt` | `created_at` | 생성 시간 (INSERT 시 자동 설정, 수정 불가) |
| `modifiedAt` | `modified_at` | 수정 시간 (UPDATE 시 자동 갱신) |

- `@Column(name = "...")`: DB 컬럼명을 명시적으로 지정하여 매핑 오류 방지
- `updatable = false`: 생성 시간은 한 번 설정 후 변경 불가

### 2. ApiResponse (공통 응답 래퍼 - 필수!)
-   **위치:** `global/response/ApiResponse.java`
-   **⚠️ 모든 API 응답은 반드시 `ApiResponse<T>`로 래핑**

```java
package com.example.project.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Schema(description = "공통 API 응답")
public class ApiResponse<T> {

    @Schema(description = "응답 코드", example = "200")
    private String code;

    @Schema(description = "응답 메시지", example = "정상적으로 완료되었습니다.")
    private String message;

    @Schema(description = "응답 데이터")
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseCode.OK.getCode(), ResponseCode.OK.getMessage(), data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResponseCode.OK.getCode(), ResponseCode.OK.getMessage(), null);
    }

    public static <T> ApiResponse<T> error(ResponseCode code) {
        return new ApiResponse<>(code.getCode(), code.getMessage(), null);
    }

    public static <T> ApiResponse<T> error(ErrorCode code) {
        // code는 HTTP 상태 코드, message는 ErrorCode의 메시지 사용
        return new ApiResponse<>(String.valueOf(code.getStatus().value()), code.getMessage(), null);
    }
}
```

### 3. ResponseCode / ErrorCode (응답 코드 관리)
-   **위치:** `global/response/ResponseCode.java`, `global/response/ErrorCode.java`
-   **ResponseCode:** 공통 HTTP 응답 코드 (200, 201, 400, 401, 500)
-   **ErrorCode:** 도메인별 비즈니스 에러 코드 (도메인 접두사 사용)

```java
// ResponseCode
package com.example.project.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ResponseCode {
    OK("200", HttpStatus.OK, "정상적으로 완료되었습니다."),
    CREATED("201", HttpStatus.CREATED, "정상적으로 생성되었습니다."),
    BAD_REQUEST("400", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED("401", HttpStatus.UNAUTHORIZED, "권한 정보가 없습니다."),
    INTERNAL_SERVER_ERROR("500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러 입니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ResponseCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
```

```java
// ErrorCode - 도메인별 접두사: Post(P), User(U), Comment(C) 등
package com.example.project.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 게시글 (P)
    NOT_FOUND_POST("P001", HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),
    INVALID_POST_CONTENT("P002", HttpStatus.BAD_REQUEST, "게시글 내용은 1자 이상 280자 이하여야 합니다."),
    UNAUTHORIZED_POST_ACCESS("P003", HttpStatus.FORBIDDEN, "해당 게시글에 대한 권한이 없습니다.");

    // 사용자 (U) - 예시
    // NOT_FOUND_USER("U001", HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
```

### 4. 예외 처리 (Exception Handling - 필수!)

**⚠️ 모든 비즈니스 예외는 반드시 `BusinessException`을 사용하고, `GlobalExceptionHandler`에서 처리해야 합니다.**

#### BusinessException
-   **위치:** `global/exception/BusinessException.java`
-   **⚠️ 절대 `IllegalArgumentException`, `RuntimeException` 등을 직접 던지지 마세요!**

```java
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

#### GlobalExceptionHandler
-   **위치:** `global/exception/GlobalExceptionHandler.java`
-   **⚠️ `@RestControllerAdvice`로 전역 예외 처리**

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // BusinessException 처리 - 도메인별 ErrorCode 사용
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException: {} - {}", errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode));
    }

    // 예상치 못한 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected exception: ", e);
        return ResponseEntity
                .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
    }
}
```

#### Service에서 예외 던지기 (올바른 예시)

```java
// ❌ 잘못된 예시 - 500 에러 발생!
public PostResponse getPost(Long id) {
    return postRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Post not found"));
}

// ✅ 올바른 예시 - 도메인 ErrorCode 사용
public PostResponse getPost(Long id) {
    return postRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_POST));
}
```

#### 예외 처리 응답 예시

```json
// 존재하지 않는 게시글 조회 시 (404)
{
    "code": "404",
    "message": "존재하지 않는 게시글입니다.",
    "data": null
}

// 게시글 내용 유효성 실패 시 (400)
{
    "code": "400",
    "message": "게시글 내용은 1자 이상 280자 이하여야 합니다.",
    "data": null
}
```

**⚠️ code는 HTTP 상태 코드, message는 ErrorCode의 메시지 사용**
- 내부적으로는 P001, P002 등으로 관리 (로깅, 디버깅용)
- 응답에서는 404, 400 등 HTTP 상태 코드로 통일 (문서화 간소화)

### 5. SpringDoc (Swagger 설정)
-   **위치:** `global/config/SpringDoc.java`
-   **⚠️ 버전:** `springdoc-openapi-starter-webmvc-ui:2.8.0` 사용
-   도메인별 API 그룹화
-   Swagger UI 접속: `http://localhost:8080/swagger-ui.html`

```java
@Configuration
@OpenAPIDefinition(info = @Info(title = "Twitter Clone API 서버", version = "v1"))
public class SpringDoc {
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public GroupedOpenApi postApi() {
        return GroupedOpenApi.builder()
                .group("post")
                .pathsToMatch("/api/posts/**")
                .build();
    }
}
```

## 📖 Swagger 문서화 (필수!)

Swagger UI에서 API를 테스트할 수 있도록 **모든 API에 문서화 어노테이션을 적용**해야 합니다.

### 주요 어노테이션

| 어노테이션 | 위치 | 설명 |
|-----------|------|------|
| `@Tag` | Controller 클래스 | API 그룹화 (메뉴 구분) |
| `@Operation` | Controller 메서드 | API 요약 및 설명 |
| `@ApiResponses` | Controller 메서드 | 응답 코드별 설명 |
| `@Parameter` | 메서드 파라미터 | 파라미터 설명 |
| `@Schema` | DTO 클래스/필드 | 모델 스키마 정보 |

### Controller 문서화 예시
```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@Tag(name = "Post", description = "게시글 API")
public class PostController {

    // 게시글 전체 조회 API (페이징)
    @GetMapping("/api/posts")
    @Operation(summary = "게시글 전체 조회", description = "페이징을 적용하여 게시글 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getAllPosts(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        // ...
    }

    // 게시글 단건 조회 API (에러 응답 예시 포함)
    @GetMapping("/api/posts/{id}")
    @Operation(summary = "게시글 단건 조회", description = "게시글 ID로 특정 게시글을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"code\": \"404\", \"message\": \"존재하지 않는 게시글입니다.\", \"data\": null}")))
    })
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable Long id) {
        // ...
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
        // ...
    }
}
```

**⚠️ 에러 응답 설정 핵심:**
- `@ExampleObject`로 직접 JSON 예시를 지정하면 `data: null`이 정확히 표시됨
- `@Schema(implementation = ...)`은 `data: "string"`으로 표시되므로 사용 지양

### DTO 문서화 예시 (@Schema)
```java
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 작성/수정 요청")
public record PostRequest(
        @Schema(description = "게시글 내용", example = "오늘 날씨가 좋네요!", maxLength = 280)
        String content,

        @Schema(description = "작성자", example = "홍길동")
        String author
) {
    // ...
}

@Schema(description = "게시글 응답")
public record PostResponse(
        @Schema(description = "게시글 ID", example = "1")
        Long id,

        @Schema(description = "게시글 내용", example = "오늘 날씨가 좋네요!")
        String content,

        @Schema(description = "작성자", example = "홍길동")
        String author,

        @Schema(description = "생성 시간", example = "2024-01-15T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 시간", example = "2024-01-15T11:00:00")
        LocalDateTime modifiedAt
) {
    // ...
}
```

### @Schema 주요 속성
| 속성 | 설명 | 예시 |
|------|------|------|
| `description` | 필드 설명 | `"게시글 내용"` |
| `example` | 예시 값 (Swagger UI에 표시) | `"오늘 날씨가 좋네요!"` |
| `maxLength` | 최대 길이 | `280` |
| `required` | 필수 여부 | `true` |

## 🎮 컨트롤러 (Controller)

-   **위치:** `{domain}/controller/` 패키지
-   `@RestController` 사용.
-   클래스 레벨에 `@RequestMapping` 금지. 메서드에 전체 경로 명시.
-   **⚠️ 반환 타입은 항상 `ResponseEntity<ApiResponse<T>>` 사용**
-   네이밍: `{Domain}Controller`
-   **✅ 주석 필수:** 각 엔드포인트 메서드 바로 위에 API 기능을 설명하는 한 줄 주석을 작성하세요.

### ResponseEntity 상태 코드 (중요!)
-   **⚠️ 반드시 `ResponseEntity.status(HttpStatus.XXX).body(ApiResponse.success(...))` 형식 사용**
-   `.ok()` 같은 축약형 금지

| HTTP Method | 상태 코드 | 사용법 |
|-------------|-----------|--------|
| GET | `HttpStatus.OK` (200) | `ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(data))` |
| POST | `HttpStatus.CREATED` (201) | `ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data))` |
| PUT/PATCH | `HttpStatus.OK` (200) | `ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(data))` |
| DELETE | `HttpStatus.OK` (200) | `ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success())` |

### 컨트롤러 예시
```java
package com.apiece.twitter.post.controller;

import com.apiece.twitter.global.response.ApiResponse;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 게시글 전체 조회 API (페이징)
    @GetMapping("/api/posts")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(postService.getAllPosts(pageable)));
    }

    // 게시글 단건 조회 API
    @GetMapping("/api/posts/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(postService.getPost(id)));
    }

    // 게시글 작성 API
    @PostMapping("/api/posts")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(@RequestBody PostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(postService.createPost(request)));
    }

    // 게시글 수정 API
    @PutMapping("/api/posts/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(@PathVariable Long id, @RequestBody PostRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(postService.updatePost(id, request)));
    }

    // 게시글 삭제 API
    @DeleteMapping("/api/posts/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success());
    }
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
-   **⚠️ BaseEntity 필드(createdAt, modifiedAt) 포함 필수**
-   Service에서 `PostResponse.from(entity)` 또는 `.map(PostResponse::from)` 형태로 사용
    ```java
    package com.apiece.twitter.post.dto;

    public record PostResponse(
            Long id,
            String content,
            String author,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt
    ) {
        public static PostResponse from(Post post) {
            return new PostResponse(
                    post.getId(),
                    post.getContent(),
                    post.getAuthor(),
                    post.getCreatedAt(),
                    post.getModifiedAt()
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
-   **⚠️ `BaseEntity` 상속 필수**
-   `protected` 기본 생성자 필수.
-   `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
-   모든 연관관계는 `FetchType.LAZY`.
-   `@JoinColumn` 사용 (물리적 FK 제약은 상황에 따라 제외 가능).
-   **⚠️ `@Setter` 금지** - 비즈니스 메서드로 상태 변경

```java
package com.example.project.post.domain;

import com.example.project.global.jpa.entity.BaseEntity;
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

    @Column(nullable = false, length = 50)
    private String author;

    @Builder
    public Post(String content, String author) {
        this.content = content;
        this.author = author;
    }

    // 비즈니스 메서드 - Setter 대신 사용
    public void updateContent(String content) {
        this.content = content;
    }
}
```

### 2. 리포지토리 (Repository)
-   **위치:** `{domain}/repository/` 패키지
-   `JpaRepository<Entity, ID>` 확장.

```java
package com.example.project.post.repository;

import com.example.project.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    // 커스텀 쿼리 메서드 예시
    // List<Post> findByAuthor(String author);
    // Page<Post> findByContentContaining(String keyword, Pageable pageable);
}
```

### 3. 서비스 (Service)
-   **위치:** `{domain}/service/` 패키지
-   **클래스 레벨:** `@Transactional(readOnly = true)` 적용
-   **쓰기(Create, Update, Delete):** 메서드에 `@Transactional` 추가
-   **⚠️ 예외는 반드시 `BusinessException` 사용**
-   **✅ 주석 필수:** 주요 비즈니스 로직 메서드 위에 **기능 요약 주석**을 작성하세요.

```java
package com.example.project.post.service;

import com.example.project.global.exception.BusinessException;
import com.example.project.global.response.ErrorCode;
import com.example.project.post.domain.Post;
import com.example.project.post.dto.PostRequest;
import com.example.project.post.dto.PostResponse;
import com.example.project.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    // 전체 게시글을 최신순으로 조회 (페이징)
    public Page<PostResponse> getAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable)
                .map(PostResponse::from);
    }

    // ID로 게시글 단건 조회
    public PostResponse getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_POST));
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
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_POST));
        post.updateContent(request.content());
        return PostResponse.from(post);
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_POST));
        postRepository.delete(post);
    }
}
```

## 🔐 인증/인가 (Authentication & Authorization)

### 개요
-   **인증 방식:** JWT (Access Token + Refresh Token)
-   **권한 수준:** 단순 인증 (로그인 여부만 체크)
-   **라이브러리:** Spring Security + jjwt

### 패키지 구조 (global/security/)

```
global/security/
├── config/
│   └── SecurityConfig.java         # Spring Security 설정
├── jwt/
│   ├── JwtTokenProvider.java       # JWT 생성/검증
│   ├── JwtAuthenticationFilter.java # JWT 인증 필터
│   └── JwtProperties.java          # JWT 설정값 (application.yaml)
├── handler/
│   ├── CustomAuthenticationEntryPoint.java  # 401 처리
│   └── CustomAccessDeniedHandler.java       # 403 처리
└── dto/
    ├── TokenRequest.java           # 로그인 요청
    └── TokenResponse.java          # 토큰 응답
```

### 의존성 (build.gradle.kts)

```kotlin
dependencies {
    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Test - Security
    testImplementation("org.springframework.security:spring-security-test")
}
```

### application.yaml 설정

```yaml
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-here-must-be-at-least-32-characters}
  access-token-validity: 3600000      # 1시간 (ms)
  refresh-token-validity: 604800000   # 7일 (ms)
```

### 1. JwtProperties (설정값 바인딩)

```java
package com.example.project.global.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenValidity,
        long refreshTokenValidity
) {}
```

**⚠️ Application 클래스에 `@EnableConfigurationProperties(JwtProperties.class)` 추가 필수!**

### 2. JwtTokenProvider (토큰 생성/검증)

```java
package com.example.project.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    // Access Token 생성
    public String createAccessToken(Long userId) {
        return createToken(userId, jwtProperties.accessTokenValidity());
    }

    // Refresh Token 생성
    public String createRefreshToken(Long userId) {
        return createToken(userId, jwtProperties.refreshTokenValidity());
    }

    private String createToken(Long userId, long validity) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validity);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    // 토큰에서 userId 추출
    public Long getUserId(String token) {
        return Long.parseLong(
                Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject()
        );
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

### 3. JwtAuthenticationFilter (인증 필터)

```java
package com.example.project.global.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserId(token);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userId, null, Collections.emptyList()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
```

### 4. 예외 핸들러 (401, 403)

```java
// CustomAuthenticationEntryPoint.java - 인증 실패 (401)
package com.example.project.global.security.handler;

import com.example.project.global.response.ApiResponse;
import com.example.project.global.response.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> errorResponse = ApiResponse.error(ErrorCode.UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
```

```java
// CustomAccessDeniedHandler.java - 권한 부족 (403)
package com.example.project.global.security.handler;

import com.example.project.global.response.ApiResponse;
import com.example.project.global.response.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> errorResponse = ApiResponse.error(ErrorCode.FORBIDDEN);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
```

### 5. SecurityConfig (보안 설정)

```java
package com.example.project.global.security.config;

import com.example.project.global.security.handler.CustomAccessDeniedHandler;
import com.example.project.global.security.handler.CustomAuthenticationEntryPoint;
import com.example.project.global.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    // 인증 없이 접근 가능한 경로
    private static final String[] PUBLIC_URLS = {
            // Swagger
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            // 인증 API
            "/api/auth/**",
            // 공개 API (필요시 추가)
            "/api/posts/**"  // 예시: 게시글 조회는 공개
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF 비활성화 (JWT 사용)
                .csrf(AbstractHttpConfigurer::disable)
                // 세션 사용 안함 (Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated()
                )
                // 예외 처리
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 6. ErrorCode 확장 (인증 관련)

```java
// ErrorCode.java에 추가
public enum ErrorCode {
    // ... 기존 코드 ...

    // 인증 (AUTH)
    UNAUTHORIZED("AUTH001", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN("AUTH002", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN("AUTH003", HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    FORBIDDEN("AUTH004", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 사용자 (U)
    NOT_FOUND_USER("U001", HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    DUPLICATE_EMAIL("U002", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD("U003", HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");

    // ...
}
```

### 7. Swagger JWT 인증 설정

```java
// SpringDoc.java 수정
package com.example.project.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Twitter Clone API 서버", version = "v1"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SpringDoc {

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/api/**")
                .addOpenApiCustomizer(openApi ->
                        openApi.addSecurityItem(new SecurityRequirement().addList("bearerAuth")))
                .build();
    }

    // ... 기존 코드 ...
}
```

### 8. 컨트롤러에서 인증된 사용자 정보 사용

```java
// Controller에서 현재 로그인한 사용자 ID 가져오기
@GetMapping("/api/me")
public ResponseEntity<ApiResponse<UserResponse>> getMe(
        @AuthenticationPrincipal Long userId) {  // SecurityContext에서 자동 주입
    return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.success(userService.getUser(userId)));
}

// 또는 직접 SecurityContext에서 가져오기
public Long getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return (Long) auth.getPrincipal();
}
```

### 9. 인증 필요 여부 표시 (Swagger)

```java
// 인증이 필요한 API
@Operation(summary = "내 정보 조회", security = @SecurityRequirement(name = "bearerAuth"))
@GetMapping("/api/me")
public ResponseEntity<ApiResponse<UserResponse>> getMe(...) { }

// 인증이 필요 없는 API (security 생략 또는 빈 배열)
@Operation(summary = "게시글 목록 조회")
@GetMapping("/api/posts")
public ResponseEntity<ApiResponse<Page<PostResponse>>> getAllPosts(...) { }
```

### 인증 관련 규칙 요약

| 항목 | 규칙 |
|------|------|
| 토큰 위치 | `Authorization: Bearer {token}` 헤더 |
| 토큰 타입 | Access Token (1시간), Refresh Token (7일) |
| 비밀번호 | BCryptPasswordEncoder 필수 |
| 공개 API | SecurityConfig의 `PUBLIC_URLS`에 등록 |
| 인증 실패 | 401 + ErrorCode.UNAUTHORIZED |
| 권한 부족 | 403 + ErrorCode.FORBIDDEN |

---

## 🌐 OAuth2 소셜 로그인 (Google, Kakao, Naver)

### 개요
-   JWT 인증과 함께 소셜 로그인 지원
-   소셜 로그인 성공 시 자체 JWT 토큰 발급
-   기존 회원과 소셜 계정 연동 가능

### 패키지 구조 (확장)

```
global/security/
├── config/
│   └── SecurityConfig.java
├── jwt/
│   └── ... (기존 JWT 관련)
├── oauth2/
│   ├── CustomOAuth2UserService.java       # 소셜 로그인 처리
│   ├── OAuth2SuccessHandler.java          # 로그인 성공 핸들러
│   ├── OAuth2FailureHandler.java          # 로그인 실패 핸들러
│   └── userinfo/
│       ├── OAuth2UserInfo.java            # 공통 인터페이스
│       ├── GoogleUserInfo.java
│       ├── KakaoUserInfo.java
│       └── NaverUserInfo.java
└── ...
```

### 의존성 추가 (build.gradle.kts)

```kotlin
dependencies {
    // OAuth2 Client
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
}
```

### application.yaml 설정

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope:
              - email
              - profile
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            authorization-grant-type: authorization_code
            client-authentication-method: client_secret_post
            scope:
              - profile_nickname
              - account_email
          naver:
            client-id: ${NAVER_CLIENT_ID}
            client-secret: ${NAVER_CLIENT_SECRET}
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            authorization-grant-type: authorization_code
            scope:
              - name
              - email
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id
          naver:
            authorization-uri: https://nid.naver.com/oauth2.0/authorize
            token-uri: https://nid.naver.com/oauth2.0/token
            user-info-uri: https://openapi.naver.com/v1/nid/me
            user-name-attribute: response
```

### 1. OAuth2UserInfo (공통 인터페이스)

```java
package com.example.project.global.security.oauth2.userinfo;

public interface OAuth2UserInfo {
    String getProviderId();    // 소셜 제공자의 고유 ID
    String getProvider();      // google, kakao, naver
    String getEmail();
    String getName();
}
```

### 2. Provider별 구현체

```java
// GoogleUserInfo.java
public class GoogleUserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    public GoogleUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }
}
```

```java
// KakaoUserInfo.java
public class KakaoUserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getEmail() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        return kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getName() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        return properties != null ? (String) properties.get("nickname") : null;
    }
}
```

```java
// NaverUserInfo.java
public class NaverUserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    @SuppressWarnings("unchecked")
    public NaverUserInfo(Map<String, Object> attributes) {
        this.attributes = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getProvider() {
        return "naver";
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }
}
```

### 3. CustomOAuth2UserService

```java
package com.example.project.global.security.oauth2;

import com.example.project.global.security.oauth2.userinfo.*;
import com.example.project.user.domain.User;
import com.example.project.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        // 사용자 조회 또는 생성
        User user = userRepository.findByProviderAndProviderId(
                userInfo.getProvider(),
                userInfo.getProviderId()
        ).orElseGet(() -> createUser(userInfo));

        // userId를 attributes에 추가
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("userId", user.getId());

        return new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                userRequest.getClientRegistration()
                        .getProviderDetails()
                        .getUserInfoEndpoint()
                        .getUserNameAttributeName()
        );
    }

    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> new GoogleUserInfo(attributes);
            case "kakao" -> new KakaoUserInfo(attributes);
            case "naver" -> new NaverUserInfo(attributes);
            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        };
    }

    private User createUser(OAuth2UserInfo userInfo) {
        User user = User.builder()
                .email(userInfo.getEmail())
                .name(userInfo.getName())
                .provider(userInfo.getProvider())
                .providerId(userInfo.getProviderId())
                .build();
        return userRepository.save(user);
    }
}
```

### 4. OAuth2SuccessHandler (로그인 성공 → JWT 발급)

```java
package com.example.project.global.security.oauth2;

import com.example.project.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private static final String REDIRECT_URI = "http://localhost:3000/oauth/callback";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long userId = (Long) oAuth2User.getAttributes().get("userId");

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // 프론트엔드로 토큰 전달 (리다이렉트)
        String redirectUrl = UriComponentsBuilder.fromUriString(REDIRECT_URI)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
```

### 5. OAuth2FailureHandler

```java
package com.example.project.global.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final String REDIRECT_URI = "http://localhost:3000/oauth/callback";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String redirectUrl = UriComponentsBuilder.fromUriString(REDIRECT_URI)
                .queryParam("error", exception.getMessage())
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
```

### 6. SecurityConfig 수정 (OAuth2 추가)

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    private static final String[] PUBLIC_URLS = {
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
            "/api/auth/**",
            "/oauth2/**", "/login/oauth2/**"  // OAuth2 관련 경로 추가
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
```

### 7. User 엔티티 (소셜 로그인 지원)

```java
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String email;

    @Column(length = 50)
    private String name;

    @Column(length = 255)
    private String password;  // 일반 로그인용 (소셜은 null)

    @Column(length = 20)
    private String provider;  // google, kakao, naver (일반 로그인은 null)

    @Column(length = 100)
    private String providerId;  // 소셜 제공자의 고유 ID

    @Builder
    public User(String email, String name, String password, String provider, String providerId) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.provider = provider;
        this.providerId = providerId;
    }
}
```

### 8. UserRepository (소셜 로그인 조회)

```java
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}
```

### 소셜 로그인 흐름 요약

```
1. 프론트엔드: /oauth2/authorization/{provider} 로 리다이렉트
   예: /oauth2/authorization/google

2. Spring Security가 자동으로 소셜 로그인 페이지로 이동

3. 사용자가 소셜 로그인 완료

4. 콜백: /login/oauth2/code/{provider}
   → CustomOAuth2UserService.loadUser() 호출
   → 사용자 조회/생성

5. OAuth2SuccessHandler 실행
   → JWT 토큰 생성
   → 프론트엔드로 리다이렉트 (토큰 포함)
   예: http://localhost:3000/oauth/callback?accessToken=xxx&refreshToken=xxx

6. 프론트엔드: 토큰 저장 후 API 요청 시 사용
```

### 소셜 로그인 관련 ErrorCode 추가

```java
// ErrorCode.java에 추가
OAUTH2_AUTHENTICATION_FAILED("AUTH010", HttpStatus.UNAUTHORIZED, "소셜 로그인에 실패했습니다."),
UNSUPPORTED_OAUTH2_PROVIDER("AUTH011", HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인입니다.");
```

### 프론트엔드 연동 예시

```javascript
// 소셜 로그인 버튼 클릭
const handleSocialLogin = (provider) => {
  window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
};

// 콜백 페이지에서 토큰 처리
useEffect(() => {
  const params = new URLSearchParams(window.location.search);
  const accessToken = params.get('accessToken');
  const refreshToken = params.get('refreshToken');

  if (accessToken) {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    navigate('/');
  }
}, []);
```

## 🛡️ 협업 및 작업 범위 규칙 (중요!)

이 프로젝트는 **팀 프로젝트**이므로 다음 규칙을 엄격히 준수하세요:

1.  **범위 확인:** 작업 시작 전, 사용자가 어떤 도메인(기능)을 작업 중인지 파악하세요.
2.  **격리 (Isolation):** 사용자의 작업 도메인 외부 패키지는 **절대 수정하지 마세요.**
    -   (예: `post/` 작업 중이면 `user/` 패키지 건드리지 말 것)
3.  **동의 구하기:** `global/` 모듈이나 다른 사람의 도메인을 수정해야 한다면 반드시 먼저 물어보세요.

## 🗄️ 데이터베이스 설정 (Database Configuration)

### 환경 분리 전략
-   **운영/개발 환경:** MySQL 사용 (`src/main/resources/application.yaml`)
-   **테스트 환경:** H2 인메모리 사용 (`src/test/resources/application.yaml`)

### build.gradle.kts 의존성
```kotlin
dependencies {
    // MySQL - 운영용
    runtimeOnly("com.mysql:mysql-connector-j")
    // H2 - 테스트용
    runtimeOnly("com.h2database:h2")
    // SpringDoc - Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0")
}
```

### 운영/개발용 설정 (src/main/resources/application.yaml)
```yaml
spring:
  application:
    name: {project-name}
  datasource:
    url: jdbc:mysql://localhost:3306/{database-name}
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: {username}
    password: {password}
  jpa:
    hibernate:
      ddl-auto: update  # 운영 시: validate 또는 none
    show-sql: false
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
```

### 테스트용 설정 (src/test/resources/application.yaml)
```yaml
spring:
  application:
    name: {project-name}
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: create-drop  # 테스트: 매번 초기화
    show-sql: true
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
```

### ddl-auto 옵션 가이드
| 환경 | 설정 | 설명 |
|------|------|------|
| 테스트 | `create-drop` | 테스트 시작 시 생성, 종료 시 삭제 |
| 개발 | `update` | 스키마 자동 업데이트 (주의 필요) |
| 운영 | `validate` | 스키마 검증만, 변경 불가 |
| 운영 | `none` | JPA 스키마 관리 비활성화 |

### 주의사항
-   **⚠️ 운영 환경에서는 절대 `create`, `create-drop`, `update` 사용 금지!**
-   테스트와 운영 설정 파일은 반드시 분리할 것
-   민감한 정보(password 등)는 환경변수 또는 외부 설정 사용 권장

## 🔨 빌드 설정 (Build Configuration)

### 빌드 전 필수 확인사항

#### 1. ddl-auto 설정 변경 (필수!)
**⚠️ 빌드 시 반드시 `application.yaml`의 `ddl-auto`를 `update`로 변경하세요.**

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 빌드 시 반드시 update로!
```

> **주의**: `create`, `create-drop` 설정으로 빌드하면 제대로 실행이 안됩니다.

#### 2. 일반 JAR 생성 비활성화 (필수!)
실행 가능한 Boot JAR만 필요하므로 `build.gradle.kts`에 다음 설정을 추가하세요.

```kotlin
tasks.jar {
    enabled = false
}
```

**왜 필요한가?**
- 이 설정이 없으면 빌드 시 두 개의 JAR 파일이 생성됨:
  - `프로젝트명-버전.jar` (실행 가능한 Boot JAR)
  - `프로젝트명-버전-plain.jar` (일반 JAR, 실행 불가)
- 위 설정으로 실행 가능한 Boot JAR만 생성됩니다.

## 🧪 테스트 코드 자동 생성 (필수!)

**⚠️ 새로운 도메인(post, user 등)을 생성할 때 반드시 테스트 코드도 함께 생성해야 합니다.**

### 테스트 패키지 구조

```
src/test/java/com/{project}/{domain}/
├── domain/
│   └── {Domain}Test.java           # 엔티티 단위 테스트
├── repository/
│   └── {Domain}RepositoryTest.java # 리포지토리 테스트 (@DataJpaTest)
├── service/
│   └── {Domain}ServiceTest.java    # 서비스 단위 테스트 (Mockito)
└── controller/
    └── {Domain}ControllerTest.java # 컨트롤러 통합 테스트 (@SpringBootTest)
```

### 테스트 환경 설정

-   **테스트 DB:** H2 인메모리 (`src/test/resources/application.yaml`)
-   **ddl-auto:** `create-drop` (테스트마다 초기화)

### 1. 엔티티 테스트 ({Domain}Test.java)

-   **어노테이션:** 없음 (순수 단위 테스트)
-   **테스트 항목:** 엔티티 생성, 비즈니스 메서드

```java
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
```

### 2. 리포지토리 테스트 ({Domain}RepositoryTest.java)

-   **어노테이션:** `@DataJpaTest`
-   **테스트 항목:** CRUD, 페이징, 커스텀 쿼리

```java
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
```

### 3. 서비스 테스트 ({Domain}ServiceTest.java)

-   **어노테이션:** `@ExtendWith(MockitoExtension.class)`
-   **Mock:** `@Mock` (Repository), `@InjectMocks` (Service)
-   **테스트 항목:** 비즈니스 로직, 예외 처리

```java
package com.apiece.twitter.post.service;

import com.apiece.twitter.global.exception.BusinessException;
import com.apiece.twitter.global.response.ErrorCode;
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
    }

    @Test
    @DisplayName("게시글 단건 조회 - 존재하지 않는 게시글")
    void getPost_NotFound() {
        // given
        Long postId = 999L;
        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.getPost(postId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_POST);
                });
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
        verify(postRepository).save(any(Post.class));
    }

    // 테스트용 Post 엔티티 생성 헬퍼 메서드 (Reflection 사용)
    private Post createPost(Long id, String content, String author) {
        Post post = Post.builder()
                .content(content)
                .author(author)
                .build();
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
```

### 4. 컨트롤러 테스트 ({Domain}ControllerTest.java)

-   **어노테이션:** `@SpringBootTest`, `@AutoConfigureMockMvc`
-   **Mock:** `@MockitoBean` (Service) - Spring Boot 3.4+ 사용
-   **테스트 항목:** API 엔드포인트, HTTP 상태 코드, 응답 형식

```java
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
import static org.mockito.BDDMockito.given;
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
                .andExpect(jsonPath("$.data.content").isArray());
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
                .andExpect(jsonPath("$.code").value("200"))  // ApiResponse.success()는 항상 "200"
                .andExpect(jsonPath("$.data.content").value("새 게시글"));
    }

    // 테스트용 PostResponse 생성 헬퍼 메서드
    private PostResponse createPostResponse(Long id, String content, String author) {
        return new PostResponse(id, content, author, LocalDateTime.now(), LocalDateTime.now());
    }
}
```

### 테스트 작성 규칙

1.  **Given-When-Then 패턴:** 모든 테스트는 `// given`, `// when`, `// then` 구조 사용
2.  **DisplayName:** `@DisplayName`으로 한글 테스트 설명 작성
3.  **AssertJ:** `assertThat()` 사용 (JUnit Assertions 대신)
4.  **BDDMockito:** `given().willReturn()` 스타일 사용

### 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 특정 도메인 테스트만 실행
./gradlew test --tests "com.apiece.twitter.post.*"

# 특정 테스트 클래스만 실행
./gradlew test --tests "com.apiece.twitter.post.service.PostServiceTest"
```

## 📜 curl 테스트 스크립트

API 개발 시 `src/main/resources/http/` 경로에 curl 스크립트 생성을 권장합니다.

### 예시: posts.http (IntelliJ HTTP Client)
```http
### 게시글 전체 조회
GET http://localhost:8080/api/posts?page=0&size=10

### 게시글 단건 조회
GET http://localhost:8080/api/posts/1

### 게시글 작성
POST http://localhost:8080/api/posts
Content-Type: application/json

{
  "content": "새 게시글 내용입니다.",
  "author": "홍길동"
}

### 게시글 수정
PUT http://localhost:8080/api/posts/1
Content-Type: application/json

{
  "content": "수정된 내용입니다.",
  "author": "홍길동"
}

### 게시글 삭제
DELETE http://localhost:8080/api/posts/1
```

### 예시: posts.sh (curl 스크립트)
```bash
#!/bin/bash
BASE_URL="http://localhost:8080/api"

# 게시글 전체 조회
curl -X GET "$BASE_URL/posts?page=0&size=10"

# 게시글 단건 조회
curl -X GET "$BASE_URL/posts/1"

# 게시글 작성
curl -X POST "$BASE_URL/posts" \
  -H "Content-Type: application/json" \
  -d '{"content": "새 게시글", "author": "홍길동"}'

# 게시글 수정
curl -X PUT "$BASE_URL/posts/1" \
  -H "Content-Type: application/json" \
  -d '{"content": "수정된 내용", "author": "홍길동"}'

# 게시글 삭제
curl -X DELETE "$BASE_URL/posts/1"
```
