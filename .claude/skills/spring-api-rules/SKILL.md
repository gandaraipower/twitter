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
│   ├── jpa/
│   │   └── entity/
│   │       └── BaseEntity.java  # 공통 엔티티 (createDate, modifyDate)
│   └── response/
│       ├── ApiResponse.java     # 공통 응답 래퍼
│       ├── ErrorCode.java       # 도메인별 에러 코드
│       └── ResponseCode.java    # 공통 응답 코드
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
@AllArgsConstructor
@Getter
public class ApiResponse<T> {
    private String code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) { ... }
    public static <T> ApiResponse<T> success() { ... }
    public static <T> ApiResponse<T> error(ResponseCode code) { ... }
    public static <T> ApiResponse<T> error(ErrorCode code) { ... }
}
```

### 3. ResponseCode / ErrorCode (응답 코드 관리)
-   **위치:** `global/response/ResponseCode.java`, `global/response/ErrorCode.java`
-   **ResponseCode:** 공통 HTTP 응답 코드 (200, 201, 400, 401, 500)
-   **ErrorCode:** 도메인별 비즈니스 에러 코드 (도메인 접두사 사용)

```java
// ResponseCode
@Getter
public enum ResponseCode {
    OK("200", HttpStatus.OK, "정상적으로 완료되었습니다."),
    CREATED("201", HttpStatus.CREATED, "정상적으로 생성되었습니다."),
    BAD_REQUEST("400", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED("401", HttpStatus.UNAUTHORIZED, "권한 정보가 없습니다."),
    INTERNAL_SERVER_ERROR("500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러 입니다.");
    // ...
}

// ErrorCode - 도메인별 접두사: Post(P), User(U), Comment(C) 등
@Getter
public enum ErrorCode {
    // 게시글 (P)
    NOT_FOUND_POST("P001", HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),
    INVALID_POST_CONTENT("P002", HttpStatus.BAD_REQUEST, "게시글 내용은 1자 이상 280자 이하여야 합니다."),
    UNAUTHORIZED_POST_ACCESS("P003", HttpStatus.FORBIDDEN, "해당 게시글에 대한 권한이 없습니다.");
    // ...
}
```

### 4. SpringDoc (Swagger 설정)
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

    // 게시글 작성 API
    @PostMapping("/api/posts")
    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponse<PostResponse>> createPost(@RequestBody PostRequest request) {
        // ...
    }
}
```

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

## 🧪 테스트 스크립트

API 개발 시 `src/main/resources/http/` 경로에 curl 스크립트 생성을 권장합니다.
-   파일명: `{resource}.sh` (예: `posts.sh`)
