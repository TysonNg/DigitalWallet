# 🏛️ Digital Wallet Architecture

Dự án **Digital Wallet** được thiết kế theo trường phái **Domain-Driven Design (DDD)** kết hợp **Hexagonal Architecture (Ports and Adapters)** nhằm tối ưu hóa tính độc lập của nghiệp vụ tài chính, dễ kiểm thử và sẵn sàng mở rộng.

---

## 1. Kiến trúc phân tầng 5 Modules (DDD)

```mermaid
graph TD
    subgraph ClientLayer [Client Layer]
        FE[Frontend / Mobile App]
    end

    subgraph ControllerModule [digitalwallet-controller]
        REST[REST Controllers & DTOs]
        ExHandler[Global Exception Handler]
    end

    subgraph ApplicationModule [digitalwallet-application]
        UC[Use Cases / Commands]
        AppResp[Application Responses]
    end

    subgraph DomainModule [digitalwallet-domain]
        Entity[Domain Entities: User, Wallet]
        DomService[Domain Services & Ports: PasswordHasher, Repositories]
        Enums[Enums: UserStatus, WalletStatus]
    end

    subgraph InfraModule [digitalwallet-infrastructure]
        JPA[Spring Data JPA & Entities]
        Flyway[Flyway Migrations]
        Sec[Spring Security & Argon2 PasswordHasherImpl]
        RedisStore[Redis Configuration & Repositories]
    end

    subgraph StartModule [digitalwallet-start]
        AppStart[Spring Boot Main Application]
        Config[application.yaml]
    end

    FE -->|HTTP / JSON| REST
    REST --> UC
    UC --> DomainModule
    InfraModule -.->|Implements Ports| DomainModule
    StartModule --> ControllerModule
    StartModule --> ApplicationModule
    StartModule --> InfraModule
```

### Chi tiết các Module:
| Module | Trách nhiệm chính |
| :--- | :--- |
| **`digitalwallet-domain`** | **Lõi nghiệp vụ (Core Domain)**: Chứa các Entity thuần túy (`User`, `Wallet`), các quy tắc nghiệp vụ tài chính (Rich Domain Model), và các Interface Ports (`UserRepository`, `WalletRepository`, `PasswordHasher`). Hoàn toàn không phụ thuộc vào bất kỳ framework bên ngoài nào. |
| **`digitalwallet-application`** | **Tầng Ứng dụng (Orchestration)**: Hiện thực các Use Case (`RegisterUseCase`, `LoginUseCase`, `CreateWalletUseCase`, `DepositMoneyUseCase`, `WithdrawUseCase`), định nghĩa Command và Response. Điều phối luồng nghiệp vụ giữa Domain và Repository. |
| **`digitalwallet-controller`** | **Tầng Giao tiếp (Inbound Adapter)**: Expose RESTful API endpoints (`/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/wallet/**`), nhận Request DTO, validate và trả về `ApiResponse<T>` chuẩn hóa. |
| **`digitalwallet-infrastructure`** | **Tầng Hạ tầng (Outbound Adapter)**: Hiện thực hóa các interface từ Domain: Spring Data JPA, Hibernate, PostgreSQL, Flyway migration, Spring Security (Argon2), Redis Client. |
| **`digitalwallet-start`** | **Tầng Khởi chạy (Bootstrapper)**: Chứa hàm `main()` Spring Boot, kết nối và cấu hình toàn bộ ứng dụng chạy trên port `3006`. |

---

## 2. Luồng Xác thực (Authentication Flow)

```mermaid
sequenceDiagram
    autonumber
    actor User as Client (Frontend)
    participant AuthCtrl as AuthController
    participant LoginUC as LoginUseCase
    participant Hasher as PasswordHasher (Argon2)
    participant UserRepo as UserRepository (PostgreSQL)
    participant TokenProv as JwtTokenProvider
    participant Redis as Redis Cache

    User->>AuthCtrl: POST /api/v1/auth/login (email, password)
    AuthCtrl->>LoginUC: login(LoginCommand)
    LoginUC->>UserRepo: findByEmail(email)
    UserRepo-->>LoginUC: User Entity (hashedPassword)
    LoginUC->>Hasher: matches(rawPassword, hashedPassword)
    
    alt Mật khẩu không đúng
        Hasher-->>LoginUC: false
        LoginUC-->>AuthCtrl: Throw BadCredentialsException
        AuthCtrl-->>User: 400 Bad Request
    else Mật khẩu chính xác
        Hasher-->>LoginUC: true
        LoginUC->>TokenProv: generateToken(user)
        TokenProv-->>LoginUC: accessToken + refreshToken
        LoginUC->>Redis: SET refreshToken (TTL: 7 ngày)
        LoginUC-->>AuthCtrl: AuthResponse(tokens, user)
        AuthCtrl-->>User: 200 OK (AuthResponseDto)
    end
```

---

## 3. Chiến lược Kiểm soát Đồng thời & Chống Double-Spending

Trong giao dịch tiền tệ (Nạp / Rút / Chuyển tiền):
- Sử dụng **Pessimistic Locking (`SELECT ... FOR UPDATE`)** hoặc **Redis Distributed Lock (Redisson)** để đảm bảo mỗi chiếc ví tại một thời điểm chỉ được thực hiện 1 giao dịch biến động số dư duy nhất.
- Mọi giao dịch biến động số dư đều được bảo vệ trong một Spring `@Transactional` duy nhất: Trừ tiền ví + Ghi bản ghi sổ cái (Transaction Ledger).
