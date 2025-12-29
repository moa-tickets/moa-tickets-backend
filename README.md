# MOA Tickets Backend

## 기술 스택
- Java 21
- Spring Boot 4.0.1
- PostgreSQL
- JPA/Hibernate

## 환경 설정

### 1. 환경변수 파일 생성
`.env.example`을 복사하여 `.env` 파일 생성 후 DB 정보 수정

**중요:** `.env` 파일에는 주석을 사용하지 마세요. Spring Boot가 주석을 포함한 파일을 파싱하지 못할 수 있습니다.

```bash
# .env.example을 복사
cp .env.example .env

# .env 파일 수정 (주석 없이 작성)
DB_URL=jdbc:postgresql://localhost:5432/your_database
DB_USERNAME=your_username
DB_PASSWORD=your_password
SPRING_PROFILES_ACTIVE=dev
```

### 2. 실행
```bash
./gradlew bootRun
```

이 프로젝트는 `spring.config.import`를 통해 `.env` 파일을 자동으로 로드합니다.
별도의 플러그인이나 IntelliJ 환경변수 설정이 필요하지 않습니다.

## 프로파일
- dev: 개발 환경 (ddl-auto: create)
- prod: 운영 환경 (ddl-auto: validate)

## 패키지 구조
- `domain`: 도메인별 비즈니스 로직
- `system`: 공통 설정 및 유틸리티

각 도메인은 다음 구조를 따릅니다:
```
domain/{domain-name}/
  ├── controller/     # API 엔드포인트
  ├── service/        # 비즈니스 로직
  ├── repository/     # 데이터 접근
  ├── entity/         # JPA 엔티티
  ├── dto/            # 요청/응답 객체
  └── exception/      # 도메인별 에러 코드
```

## 개발 규칙

### API 응답 형식
모든 API는 `ApiResponse<T>`를 반환합니다:
```java
// 성공
return ApiResponse.success(data);

// 실패 (예외 던지기 - GlobalExceptionHandler가 자동 처리)
throw new CustomException(UserErrorCode.USER_NOT_FOUND);
```

**응답 예시:**
```json
{
  "success": true,
  "data": { ... },
  "code": null,
  "message": null,
  "timestamp": "2025-12-27T00:15:30",
  "status": 200
}
```

### 예외 처리
1. **도메인별로 `ErrorCode` enum 생성**
   ```java
   public enum UserErrorCode implements ErrorCode {
       USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다.");

       private final HttpStatus status;
       private final String code;
       private final String message;
   }
   ```

2. **예외 발생 시 `CustomException` 사용**
   ```java
   throw new CustomException(UserErrorCode.USER_NOT_FOUND);
   ```

3. **환경별 에러 메시지**
   - **dev**: 상세한 예외 메시지 노출
   - **prod**: "서버 내부 오류"로 일반화

### Entity 작성 규칙
```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public User(/* 필수 필드만 */) {
        // ...
    }
}
```

### DTO 작성 규칙
```java
// Response DTO
@Getter
@Builder
public class UserResponse {
    private Long id;
    private String name;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}

// Request DTO
@Getter
public class UserCreateRequest {
    @NotBlank(message = "이름은 필수입니다.")
    private String name;
}
```

### Controller 작성 규칙
```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return ApiResponse.success(response);
    }
}
```