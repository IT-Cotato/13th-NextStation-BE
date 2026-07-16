# Backend Code Convention

> NextStation 프로젝트 백엔드(Spring Boot) 코드 컨벤션 문서입니다.
> CodeRabbit 및 팀원 리뷰의 기준이 되며, PR 작성 전 반드시 확인해 주세요.

---

## 목차

1. [패키지 구조](#1-패키지-구조)
2. [네이밍 컨벤션](#2-네이밍-컨벤션)
3. [계층별 책임 (Layered Architecture)](#3-계층별-책임-layered-architecture)
4. [Controller 컨벤션](#4-controller-컨벤션)
5. [Service 컨벤션 & CQRS 패턴](#5-service-컨벤션--cqrs-패턴)
6. [Repository 컨벤션](#6-repository-컨벤션)
7. [Entity 컨벤션](#7-entity-컨벤션)
8. [DTO 컨벤션](#8-dto-컨벤션)
9. [Converter (Entity ↔ DTO)](#9-converter-entity--dto)
10. [예외 처리 (Exception Handling)](#10-예외-처리-exception-handling)
11. [API 응답 형식](#11-api-응답-형식)
12. [Validation](#12-validation)
13. [트랜잭션 관리](#13-트랜잭션-관리)
14. [테스트 코드](#14-테스트-코드)
15. [코드 스타일 & 포맷팅](#15-코드-스타일--포맷팅)
16. [보안](#16-보안)
17. [로깅](#17-로깅)
18. [Git 컨벤션](#18-git-컨벤션)

---

## 1. 패키지 구조

**도메인형 패키지 구조**를 기본으로 한다. 계층형(controller, service, ... 최상위 분리)보다 도메인 응집도를 우선한다.

```
com.cotato.nextstation
├── domain
│   ├── member
│   │   ├── controller
│   │   ├── service
│   │   │   ├── MemberCommandService
│   │   │   └── MemberQueryService
│   │   ├── repository
│   │   ├── entity
│   │   ├── dto
│   │   │   ├── request
│   │   │   └── response
│   │   ├── converter
│   │   └── exception
│   └── order
│       └── ...
└── global
    ├── config
    ├── common
    │   ├── response      // CommonResponse 등 공통 응답
    │   └── entity        // BaseEntity 등
    ├── exception         // 공통 예외, GlobalExceptionHandler
    ├── security
    └── util
```

- 도메인 간 직접 의존은 최소화하고, 불가피할 경우 인터페이스 또는 이벤트를 통해 결합도를 낮춘다.
- 공통으로 재사용되는 코드는 `global` 하위로 분리한다.

---

## 2. 네이밍 컨벤션

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 클래스 | PascalCase | `MemberCommandService` |
| 메서드 / 변수 | camelCase | `findMemberById`, `memberName` |
| 상수 | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| 패키지 | 소문자 | `com.nextstation.domain.member` |
| 테스트 클래스 | `{대상}Test` | `MemberServiceTest` |

- **약어는 지양**하고 의미가 드러나는 이름을 사용한다. (`memberRepo` ❌ → `memberRepository` ✅)
- boolean 필드/메서드는 `is`, `has`, `can` 접두사를 사용한다. (`isDeleted`, `hasPermission`)
- 컬렉션은 복수형으로 명명한다. (`members`, `orderIds`)

### 계층별 클래스 접미사

| 계층 | 접미사 | 예시 |
| --- | --- | --- |
| Controller | `Controller` | `MemberController` |
| Service | `CommandService` / `QueryService` | `MemberQueryService` |
| Repository | `Repository` | `MemberRepository` |
| Converter | `Converter` | `MemberConverter` |
| Request DTO | `{동작}Request` | `MemberCreateRequest` |
| Response DTO | `{용도}Response` | `MemberResponse`, `MemberSimpleResponse` |
| Exception | `Exception` | `MemberNotFoundException` |

---

## 3. 계층별 책임 (Layered Architecture)

```
Controller  →  Service  →  Repository
   (요청/응답)   (비즈니스 로직)  (영속성)
```

- **Controller**: 요청/응답 처리, 입력 검증(Validation), Service 호출. 비즈니스 로직 금지.
- **Service**: 비즈니스 로직의 중심. 트랜잭션 경계. Entity ↔ DTO 변환은 Converter에 위임.
- **Repository**: DB 접근만 담당. 비즈니스 로직 금지.
- 상위 계층은 하위 계층에만 의존하고, **역방향 의존을 금지**한다.
- Controller는 Entity를 직접 반환하지 않고 반드시 DTO로 변환하여 반환한다.

---

## 4. Controller 컨벤션

- 클래스에 `@RestController`, `@RequestMapping("/api/v1/{domain}")`를 명시한다.
- URL은 **명사 복수형 + 소문자 케밥/스네이크 지양**, RESTful 규칙을 따른다.

| 동작 | HTTP Method | URL 예시 |
| --- | --- | --- |
| 목록 조회 | GET | `/api/v1/members` |
| 단건 조회 | GET | `/api/v1/members/{memberId}` |
| 생성 | POST | `/api/v1/members` |
| 전체 수정 | PUT | `/api/v1/members/{memberId}` |
| 부분 수정 | PATCH | `/api/v1/members/{memberId}` |
| 삭제 | DELETE | `/api/v1/members/{memberId}` |

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;

    @PostMapping
    public CommonResponse<MemberResponse> createMember(
            @Valid @RequestBody MemberCreateRequest request) {
        return CommonResponse.success(memberCommandService.createMember(request));
    }

    @GetMapping("/{memberId}")
    public CommonResponse<MemberResponse> getMember(@PathVariable Long memberId) {
        return CommonResponse.success(memberQueryService.getMember(memberId));
    }
}
```

- `@RequiredArgsConstructor` + `final` 필드 기반 **생성자 주입**을 사용한다. (필드 주입 `@Autowired` 금지)
- 경로 변수는 `{domain}Id` 형태로 명명한다.

---

## 5. Service 컨벤션 & CQRS 패턴

Entity와 DTO 간의 Service 코드를 **Command와 Query로 명확히 분리**하여 책임을 구분한다.

### 정의

- **Command (명령)**: 상태를 변경하는 메서드
    - `create`, `update`, `delete`, `save`, `remove` 등
    - 반환값: `void` 또는 생성된 Entity/DTO
    - 트랜잭션 쓰기 작업
- **Query (조회)**: 상태를 변경하지 않고 데이터만 조회하는 메서드
    - `get`, `find`, `search`, `list`, `count` 등
    - 반환값: Entity/DTO 또는 컬렉션
    - 읽기 전용 작업

### 작성 규칙

- Query 메서드는 `@Transactional(readOnly = true)`를 사용한다.
- Command 메서드는 `@Transactional`을 사용한다.
- `service` 디렉토리 내 `{domain}CommandService`, `{domain}QueryService`로 작성한다.

```java
@Service
@RequiredArgsConstructor
@Transactional
public class MemberCommandService {

    private final MemberRepository memberRepository;
    private final MemberConverter memberConverter;

    public MemberResponse createMember(MemberCreateRequest request) {
        Member member = memberConverter.toEntity(request);
        Member saved = memberRepository.save(member);
        return memberConverter.toResponse(saved);
    }
}

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberRepository memberRepository;
    private final MemberConverter memberConverter;

    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        return memberConverter.toResponse(member);
    }
}
```

- Service 간 호출이 필요한 경우, 순환 참조가 발생하지 않도록 주의한다.
- Entity를 직접 노출하지 않고 DTO로 변환하여 반환한다.

---

## 6. Repository 컨벤션

- Spring Data JPA `JpaRepository`를 상속한다.
- 조회 결과가 없을 수 있는 단건 조회는 `Optional`을 반환한다.
- 쿼리 메서드 명명 규칙을 따르되, 복잡한 쿼리는 `@Query` 또는 QueryDSL을 사용한다.

```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT m FROM Member m WHERE m.status = :status")
    List<Member> findAllByStatus(@Param("status") MemberStatus status);
}
```

- N+1 문제를 유발하는 연관 조회는 `fetch join` 또는 `@EntityGraph`로 해결한다.
- 페이징이 필요한 목록 조회는 `Pageable`을 사용한다.
- Repository 계층에는 비즈니스 로직을 두지 않는다.

---

## 7. Entity 컨벤션

- `@Entity`, `@Table(name = "...")`을 명시한다. (스네이크 케이스 테이블명)
- 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 외부 무분별한 생성을 막는다.
- **`@Setter`를 지양**하고, 의미 있는 변경 메서드를 제공한다. (`updateName()`, `changeEmail()`)
- 생성은 `@Builder` 또는 정적 팩토리 메서드를 사용한다.
- 공통 필드(id)는 `BaseEntity`로 분리한다.
- 공통 필드(생성/수정시각)은 `BaseTimeEntity`로 분리한다.

```java
@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {
    
    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    @Builder
    private Member(String name, String email) {
        this.name = name;
        this.email = email;
        this.status = MemberStatus.ACTIVE;
    }

    public void updateName(String name) {
        this.name = name;
    }
}
```

```java
@Getter
@MappedSuperclass
public class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

- 연관관계는 **지연 로딩(`FetchType.LAZY`)을 기본**으로 한다.
- `@Enumerated`는 반드시 `EnumType.STRING`을 사용한다. (ORDINAL 금지)
- 양방향 연관관계는 필요할 때만 사용하고, 연관관계 편의 메서드를 제공한다.

---

## 8. DTO 컨벤션

- **`record`를 기본**으로 사용한다. (불변성 확보)
- Request/Response DTO를 분리하며, 각각 `dto.request`, `dto.response` 패키지에 위치한다.
- Entity를 그대로 노출하지 않는다.
- 정적 팩토리 메서드(`from`, `of`)로 변환을 제공할 수 있으나, 복잡하면 Converter를 사용한다.

```java
// Request
public record MemberCreateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotBlank @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email
) {}

// Response
public record MemberResponse(
        Long id,
        String name,
        String email
) {}

public record MemberSimpleResponse(
        Long id,
        String name
) {}
```

---

## 9. Converter (Entity ↔ DTO)

Entity와 DTO 간의 변환을 위해 별도의 Converter 클래스를 사용한다.

### 목적

- 변환 로직을 분리하여 Service 계층의 책임을 명확히 한다.
- 변환 로직의 재사용성을 높인다.
- 변환 규칙의 중앙화로 유지보수성을 향상시킨다.

### 네이밍 규칙

- `{domain}Converter`

### 작성 규칙

- 메서드명은 변환 방향을 명시한다. `toResponse()`, `toResponses()`, `toEntity()` 등
- 변환 로직은 간결하게 유지한다. 복잡한 비즈니스 로직은 포함하지 않고, 필드 매핑 정도만 담당한다.

```java
@Component
public class MemberConverter {

    // Entity → Response DTO
    public MemberResponse toResponse(Member member) {
        return new MemberResponse(
            member.getId(),
            member.getName(),
            member.getEmail()
        );
    }

    // Entity 리스트 → Response DTO 리스트
    public List<MemberResponse> toResponses(List<Member> members) {
        return members.stream()
            .map(this::toResponse)
            .toList();
    }

    // Request DTO → Entity
    public Member toEntity(MemberCreateRequest request) {
        return Member.builder()
            .name(request.name())
            .email(request.email())
            .build();
    }

    // 다양한 Response 타입 변환 예시
    public MemberSimpleResponse toSimpleResponse(Member member) {
        return new MemberSimpleResponse(member.getId(), member.getName());
    }
}
```

---

## 10. 예외 처리 (Exception Handling)

### 기본 원칙

- 비즈니스 예외는 공통 `CustomException`을 사용하고, 에러 정보는 `ErrorCode`(Enum)로 중앙 관리한다.
- 전역 예외는 `@RestControllerAdvice` 기반의 `GlobalExceptionHandler`에서 일괄 처리하고, 개별 `try-catch` 남용을 지양한다.
- 응답은 공통 래퍼 `CommonResponse`로 반환하여 형식을 일관되게 유지한다.
- 예외 처리 관련 클래스는 `global.exception` 패키지 하위에 위치시킨다. (`ErrorCode`는 `global.exception.error`)

### ErrorCode

- 각 도메인/전역별로 `ErrorCode` 인터페이스를 구현하는 Enum을 두어 코드를 관리한다.
- 전역 공통 에러는 `GlobalErrorCode`, 도메인 에러는 `{Domain}ErrorCode`로 작성한다.

```java
public interface ErrorCode {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_VALIDATION_ERROR", "요청 값이 유효하지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR_500_INTERNAL_SERVER_ERROR", "서버 내부 오류입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_MEMBER_NOT_FOUND", "존재하지 않는 회원입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "CLIENT_ERROR_409_DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
```

### CustomException

- 비즈니스 로직에서 예외가 필요한 경우 `ErrorCode`를 담아 `CustomException`을 던진다.
- 도메인마다 예외 클래스를 새로 만들기보다, `ErrorCode`로 구분하여 하나의 `CustomException`을 재사용한다.

```java
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

```java
// 사용 예시
Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
```

### GlobalExceptionHandler

- 검증 예외(`MethodArgumentNotValidException`)는 필드 에러와 클래스 레벨(글로벌) 에러를 모두 `Map`으로 모아 반환한다.
- 비즈니스 예외(`CustomException`)는 담긴 `ErrorCode`로 응답한다.
- Spring MVC가 던지는 표준 예외(JSON 파싱 오류, 파라미터 누락/타입 불일치, 지원하지 않는 HTTP 메서드, 존재하지 않는 경로)는 각각의 의미에 맞는 4xx로 개별 처리한다. **`Exception.class` 캐치올 하나로 뭉뚱그려 500으로 처리하지 않는다.**
- 상태 코드는 하드코딩하지 않고 `ErrorCode.getHttpStatus()`를 사용한다.
- 그 외 예상치 못한 예외만 `Exception` 핸들러에서 처리하고, `INTERNAL_SERVER_ERROR`로 응답하며 스택 트레이스를 `error` 레벨로 로깅한다.

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> reasons = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                reasons.put(error.getField(), error.getDefaultMessage())
        );
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                reasons.put(error.getObjectName(), error.getDefaultMessage())
        );
        CommonResponse<Void> response = CommonResponse.error(GlobalErrorCode.VALIDATION_ERROR, reasons);
        return ResponseEntity.status(GlobalErrorCode.VALIDATION_ERROR.getHttpStatus()).body(response);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<CommonResponse<Void>> handleCustomException(CustomException ex) {
        log.warn("CustomException: code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());
        CommonResponse<Void> response = CommonResponse.error(ex.getErrorCode());
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
    }

    // 요청 본문 JSON 파싱 실패 -> 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("HttpMessageNotReadableException: {}", ex.getMessage());
        CommonResponse<Void> response = CommonResponse.error(GlobalErrorCode.INVALID_REQUEST);
        return ResponseEntity.status(GlobalErrorCode.INVALID_REQUEST.getHttpStatus()).body(response);
    }

    // 필수 파라미터 누락 -> 400
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CommonResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.warn("MissingServletRequestParameterException: {}", ex.getMessage());
        CommonResponse<Void> response = CommonResponse.error(GlobalErrorCode.INVALID_REQUEST);
        return ResponseEntity.status(GlobalErrorCode.INVALID_REQUEST.getHttpStatus()).body(response);
    }

    // 파라미터 타입 불일치 -> 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("MethodArgumentTypeMismatchException: {}", ex.getMessage());
        CommonResponse<Void> response = CommonResponse.error(GlobalErrorCode.INVALID_REQUEST);
        return ResponseEntity.status(GlobalErrorCode.INVALID_REQUEST.getHttpStatus()).body(response);
    }

    // 지원하지 않는 HTTP 메서드 -> 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.warn("HttpRequestMethodNotSupportedException: {}", ex.getMessage());
        CommonResponse<Void> response = CommonResponse.error(GlobalErrorCode.METHOD_NOT_ALLOWED);
        return ResponseEntity.status(GlobalErrorCode.METHOD_NOT_ALLOWED.getHttpStatus()).body(response);
    }

    // 존재하지 않는 경로 -> 404
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("NoResourceFoundException: {}", ex.getMessage());
        CommonResponse<Void> response = CommonResponse.error(GlobalErrorCode.NOT_FOUND);
        return ResponseEntity.status(GlobalErrorCode.NOT_FOUND.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Unexpected error: ", ex);
        CommonResponse<Void> response = CommonResponse.error(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

### 규칙 요약

- 예외 메시지에 민감 정보(비밀번호, 토큰 등)를 포함하지 않는다.
- 예상 가능한 비즈니스 예외는 필요 시 `warn`, 예상치 못한 예외는 `error`로 로깅한다.
- HTTP 상태 코드는 `ErrorCode`의 `httpStatus`를 기준으로 내려가도록 하고, 무분별한 200 응답을 지양한다.
- 새로운 예외 케이스가 필요하면 예외 클래스를 늘리기보다 해당 도메인의 `ErrorCode`에 상수를 추가한다.

---

## 11. API 응답 형식

- 모든 API는 공통 응답 래퍼 `CommonResponse<T>`로 감싸 성공/실패 형식을 일관되게 유지한다.
- 생성자는 `private`으로 막고, 정적 팩토리 메서드(`success`, `error`)로만 생성한다.
- `null` 필드는 `@JsonInclude(JsonInclude.Include.NON_NULL)`로 응답에서 제외한다. (예: 성공 응답에는 `reasons`가 빠짐)

### 필드 구성

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `success` | `boolean` | 성공 여부 |
| `status` | `int` | HTTP 상태 코드 값 |
| `code` | `String` | 응답 코드 (`SUCCESS` 또는 `ErrorCode`의 코드) |
| `message` | `String` | 응답 메시지 |
| `data` | `T` | 응답 데이터 (실패 시 `null`) |
| `timestamp` | `LocalDateTime` | 응답 생성 시각 |
| `reasons` | `Map<String, Object>` | 검증 실패 등 상세 사유 (없으면 `null`) |

```java
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> {

    private final boolean success;
    private final int status;
    private final String code;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;
    private final Map<String, Object> reasons;

    private CommonResponse(
            boolean success,
            int status,
            String code,
            String message,
            T data,
            LocalDateTime timestamp,
            Map<String, Object> reasons
    ) {
        this.success = success;
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
        this.reasons = reasons;
    }

    // 성공 응답 (기본 200 OK)
    public static <T> CommonResponse<T> success(T data) {
        return success(HttpStatus.OK, data);
    }

    // 성공 응답 (상태코드 지정, 예: 201 Created)
    public static <T> CommonResponse<T> success(HttpStatus status, T data) {
        return new CommonResponse<>(
                true,
                status.value(),
                "SUCCESS",
                "요청이 성공적으로 처리되었습니다.",
                data,
                LocalDateTime.now(),
                null
        );
    }

    // 에러 응답
    public static CommonResponse<Void> error(ErrorCode errorCode) {
        return error(errorCode, null);
    }

    public static CommonResponse<Void> error(ErrorCode errorCode, Map<String, Object> reasons) {
        return new CommonResponse<>(
                false,
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                null,
                LocalDateTime.now(),
                reasons
        );
    }
}
```

### 사용 규칙

- Controller에서 성공 응답은 `CommonResponse.success(data)`로 반환한다. (기본 200 OK)
- 200이 아닌 상태 코드로 응답해야 하는 경우(예: 생성 API의 201 Created)에는 `CommonResponse.success(HttpStatus.CREATED, data)`를 사용해, body의 `status` 필드와 실제 HTTP 상태 코드가 일치하도록 한다.
- 에러 응답은 직접 생성하지 않고, 예외를 던져 `GlobalExceptionHandler`가 `CommonResponse.error(...)`로 변환하도록 위임한다.
- 응답 코드(`code`)는 `status`(HTTP 코드)와 별개로, 클라이언트가 케이스를 구분할 수 있는 값으로 사용한다.
- 반환 데이터가 없는 경우에도 래핑하며, 타입은 `CommonResponse<Void>`를 사용한다.

```java
@PostMapping
public CommonResponse<MemberResponse> createMember(
        @Valid @RequestBody MemberCreateRequest request) {
    return CommonResponse.success(memberCommandService.createMember(request));
}
```

---

## 12. Validation

- 입력 검증은 Controller 진입 시 `@Valid`로 수행한다.
- 검증 애노테이션은 Request DTO에 선언하고, 메시지를 명확히 작성한다.
- 단순 형식 검증은 Bean Validation, 비즈니스 규칙 검증은 Service에서 수행한다.

```java
public record MemberCreateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email
) {}
```

---

## 13. 트랜잭션 관리

- 트랜잭션 경계는 **Service 계층**에 둔다.
- 조회 전용은 `@Transactional(readOnly = true)`로 성능을 최적화한다.
- 클래스 레벨에 기본값을 두고, 메서드 레벨에서 필요 시 오버라이드한다.
- 외부 API 호출, 파일 IO 등 오래 걸리는 작업은 트랜잭션 범위 밖으로 분리한다.
- 트랜잭션 내에서 발생한 예외로 롤백이 필요한 경우, `RuntimeException` 기반 예외를 던진다. (checked 예외는 기본 롤백되지 않음에 유의)

---

## 14. 테스트 코드

- 테스트는 `given - when - then` 구조로 작성한다.
- 테스트 메서드명은 한글 또는 `@DisplayName`으로 의도를 명확히 한다.
- 계층별로 필요한 테스트를 작성한다.

| 계층 | 테스트 종류 | 설명 |
| --- | --- | --- |
| Controller | `@WebMvcTest` + MockMvc | API 요청/응답, 검증 테스트 |
| Service | 단위 테스트 (Mockito) | 비즈니스 로직 검증 |
| Repository | `@DataJpaTest` | 쿼리 메서드 검증 |

```java
@ExtendWith(MockitoExtension.class)
class MemberQueryServiceTest {

    @InjectMocks
    private MemberQueryService memberQueryService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberConverter memberConverter;

    @Test
    @DisplayName("존재하지 않는 회원을 조회하면 예외가 발생한다")
    void getMember_notFound() {
        // given
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberQueryService.getMember(memberId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
    }
}
```

- 테스트는 서로 독립적이어야 하며, 실행 순서에 의존하지 않는다.
- 외부 의존성(외부 API, 메일 등)은 Mocking한다.

---

## 15. 코드 스타일 & 포맷팅

- 들여쓰기는 **스페이스 4칸**을 사용한다.
- import 시 `*` 와일드카드를 사용하지 않는다.
- 사용하지 않는 import, 변수, 코드는 제거한다.
- 한 메서드는 하나의 책임만 갖도록 짧게 유지한다. (권장 20라인 이내)
- 매직 넘버/문자열은 상수로 추출한다.
- `Optional`을 필드나 파라미터로 남용하지 않는다. (반환 타입 위주로 사용)
- Stream은 가독성을 해치지 않는 선에서 사용하고, 과도한 체이닝을 지양한다.
- 주석은 '무엇'보다 '왜'를 설명한다. 코드로 설명 가능한 것은 주석 대신 코드로 표현한다.

---

## 16. 보안

- 비밀번호는 반드시 단방향 해시(BCrypt 등)로 저장한다. 평문 저장 금지.
- 민감 정보(DB 비밀번호, 시크릿 키 등)는 코드에 하드코딩하지 않고 환경변수/설정 파일로 분리한다.
- 응답 DTO에 비밀번호, 토큰 등 민감 필드를 포함하지 않는다.
- SQL Injection 방지를 위해 문자열 연결 쿼리 대신 파라미터 바인딩을 사용한다.
- 인증/인가는 Spring Security 필터/설정에서 처리하고, 권한 체크 로직을 도메인 곳곳에 분산시키지 않는다.
- 로그에 개인정보/민감정보를 남기지 않는다.

---

## 17. 로깅

- `System.out.println` 대신 SLF4J(`@Slf4j`) 로거를 사용한다.
- 로그 레벨을 상황에 맞게 사용한다.
    - `ERROR`: 예상치 못한 시스템 오류
    - `WARN`: 예상 가능한 예외, 주의가 필요한 상황
    - `INFO`: 주요 비즈니스 흐름
    - `DEBUG`: 개발/디버깅용 상세 정보
- 로그 메시지는 파라미터 바인딩(`log.info("id={}", id)`)을 사용한다. (문자열 연결 지양)

---

## 18. Git 컨벤션

### 커밋 메시지

```
<type>: <subject>

<body (선택)>
```

| type | 설명 |
| --- | --- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 (기능 변화 없음) |
| `test` | 테스트 코드 추가/수정 |
| `docs` | 문서 수정 |
| `style` | 포맷팅, 세미콜론 등 (로직 변화 없음) |
| `chore` | 빌드, 설정, 패키지 등 기타 작업 |

- subject는 명령형·현재형으로 간결하게 작성한다. (`feat: 회원 가입 API 추가`)

### 브랜치 전략

- `develop`: 배포 브랜치
- `feature/{이슈번호}-{기능}`: 기능 개발 브랜치
- 예: `feature/12-member-signup`

### PR 규칙

- PR은 작은 단위로 나눠 리뷰 부담을 줄인다.
- PR 본문에 변경 사항 요약과 관련 이슈를 명시한다.
- 팀 컨벤션상 **리뷰어 2명 Approve** 후 머지한다.

---

> 본 문서는 팀 논의를 통해 지속적으로 갱신됩니다. 컨벤션 변경이 필요하면 이슈로 제안해 주세요.