# Withdrawal qua Kafka

Mặc định `withdrawal.processing-mode=kafka` trong application.yaml. Kafka consumer khởi phát payout; scheduler vẫn đối soát payout đã bắt đầu và xử lý webhook inbox. Toàn bộ bước này dùng lại provider gateway, checkpoint và idempotency key hiện có.

## Flow hiện tại

```mermaid
sequenceDiagram
    participant API
    participant DB as PostgreSQL
    participant P as Outbox Publisher
    participant K as Kafka
    participant C as Withdrawal Listener
    participant U as ConsumeWithdrawalEventUseCase
    participant G as ProcessWithdrawalUseCase / Gateway
    API->>DB: Transaction: giữ tiền + withdrawal + attempt + outbox
    DB-->>API: Commit, trả withdrawal ID
    P->>DB: Khóa event đến hạn
    P->>K: Publish event, chờ ACK
    P->>DB: Lưu published_at, commit
    K->>C: Event
    C->>C: Kiểm tra JSON, type, version, UUID, aggregate và key
    C->>U: eventId, withdrawalId
    U->>U: Watchdog Redis lock theo consumer + eventId
    U->>DB: Đọc completed receipt và trạng thái withdrawal
    alt Chưa xử lý và PENDING / PROCESSING
        U->>G: Process (không có transaction ngoài bao quanh provider)
        G->>DB: Khóa withdrawal; commit PROCESSING trước lời gọi provider
        G->>G: Gọi provider với key ổn định, lưu checkpoint qua transaction ngắn
        G->>DB: Commit kết quả PAID / PENDING / FAILED / REVIEW_REQUIRED
    end
    U->>DB: Transaction riêng: INSERT inbox_event hoàn tất
    U-->>C: Thành công
    C-->>K: Container commit offset sau khi listener trả về
```

`PENDING` từ provider là một kết quả xử lý an toàn nếu checkpoint và trạng thái đã được lưu. Listener có thể hoàn tất inbox và commit offset trước khi tiền đến ngân hàng; scheduler/webhook tiếp tục đối soát. `SUCCEEDED`, `FAILED`, `RETURNED`, `REVIEW_REQUIRED` trong backlog được ghi receipt và bỏ qua bước gọi provider từ consumer. Scheduler vẫn có thể kiểm tra payout SUCCEEDED sau khoảng cutoff để phát hiện returned payout như trước.

## Chống trùng và ranh giới transaction

`inbox_event` là bảng dùng chung cho nhiều consumer. Khóa chính `(consumer_name, event_id)`, cùng `aggregate_id`, `processed_at`. Consumer hiện dùng tên logic cố định `withdrawal-process-v1`, độc lập với consumer group Kafka. Mỗi receipt chỉ biểu diễn đã hoàn tất, không có lease hoặc trạng thái in-progress cần phục hồi. Không có FK riêng tới withdrawal và không chứa payload/bank/PIN.

- Cùng event đã có receipt: bỏ qua. Nếu cùng eventId lại trỏ withdrawal khác: invalid event, chuyển DLT.
- Hai consumer cùng nhận event: Redis lock theo event ID chỉ cho một bên xử lý; bên còn lại retry. Use case payout còn có Redis lock theo withdrawal, bảo vệ cả event khác ID và scheduler.
- Lỗi xử lý hoặc lỗi insert receipt: exception truyền ra listener, không có receipt hoàn tất và container retry.
- Provider nhận payout nhưng ứng dụng dừng trước checkpoint/receipt: lần sau dùng lại cùng withdrawal identity/provider idempotency key, checkpoint và trạng thái; không dựa vào inbox để tuyên bố exactly-once.
- Business state đã commit thành công nhưng receipt thất bại: event phát lại kiểm tra trạng thái terminal và ghi receipt, không tạo payout mới.
- Inbox đã commit nhưng offset chưa commit: phát lại đọc receipt rồi bỏ qua.

Không có một transaction database kéo dài qua provider và inbox. Việc tách transaction là có chủ ý để không giữ DB lock/connection trong suốt các lời gọi bên ngoài; tính an toàn dựa trên state machine, các khóa, checkpoint và idempotency của provider. Redis phải sẵn sàng; lỗi khóa không được bỏ qua.

## Retry và DLT

Factory riêng `withdrawalKafkaListenerContainerFactory` giữ các cấu hình Boot và đặt `AckMode.RECORD`, synchronous commit, `enable.auto.commit=false`, `max.poll.records=1`, `max.poll.interval.ms=600000`. Một record mỗi poll giảm nguy cơ xử lý batch quá thời gian poll khi gọi provider.

`DefaultErrorHandler` retry lỗi tạm thời theo `spring.kafka.withdrawal-consumer.retry-interval=1s`, `retries=2`: tối đa ba lần gọi listener trong một chu kỳ retry. Retry counter này ở bộ nhớ container, có thể bắt đầu lại khi restart; đây không phải giới hạn vĩnh viễn cho event.

`InvalidWithdrawalEventException` không retry: JSON hỏng/tombstone, type hoặc version không hỗ trợ, UUID thiếu/sai, aggregateId hoặc key không khớp withdrawalId. Không phân loại mọi IllegalArgumentException là fatal để tránh đưa lỗi business/database chưa sẵn sàng vào DLT quá sớm.

Hết retry hoặc invalid event được chuyển sang topic alias `withdrawal-process-dlt`, mặc định `wallet.withdrawal.process.v1.dlt`. Topic DLT phải có ít nhất số partition của source vì recoverer giữ partition gốc. Recoverer chờ broker xác nhận (`failIfSendResultIsError=true`); DLT gửi lỗi thì source offset chưa được vượt qua record đó. Chuyển DLT cũng có thể trùng khi crash giữa ACK và commit offset.

DLT không hoàn tiền, không đánh dấu withdrawal FAILED và không tạo receipt inbox. Payout đã bắt đầu có thể vẫn được scheduler đối soát thành công. Sau khi kiểm tra/correct lỗi có thể phát lại event với cùng eventId và withdrawalId; state/receipt hiện tại quyết định xử lý hay bỏ qua. Không có automatic DLT replay consumer trong bước này.

## Chuyển giao với scheduler

| Chế độ | Listener Kafka | Scheduler |
| --- | --- | --- |
| `kafka` (mặc định YAML) | Khởi phát từ topic | Chỉ recovery các payout đã bắt đầu, delayed return checks và webhook inbox |
| `scheduler` | Không đăng ký listener | Giữ cách cũ, lấy cả PENDING để khởi phát |

Đổi bằng `WITHDRAWAL_PROCESSING_MODE`. Mode không hợp lệ làm ứng dụng fail-fast. Khi chuyển môi trường đang chạy, dừng/drain các worker chế độ cũ trước khi khởi động worker chế độ mới; không rolling deploy lẫn chế độ vì worker cũ vẫn có thể lấy PENDING.

Query recovery dùng JPA repository với native PostgreSQL: PROCESSING, SUCCEEDED đủ cutoff, hoặc legacy PENDING có attempt UNKNOWN/status đã tiến hoặc các checkpoint provider-start/compensation. Fresh PENDING chưa bắt đầu không được scheduler tự lấy trong mode Kafka. Process use case mới luôn commit PROCESSING trước provider, nên crash sau mốc này trở thành recovery work.

Với dữ liệu cũ chưa từng có outbox: fresh PENDING không tự được Kafka xử lý. Cần kiểm tra và backfill event trong transaction cho các yêu cầu này hoặc xử lý bằng kế hoạch chuyển giao scheduler có kiểm soát. Không tự backfill hoặc tự gọi payout trên database ứng dụng trong thay đổi này. Backlog có outbox vẫn được publisher gửi; consumer bỏ qua withdrawal đã hoàn tất.

## File

| Layer | File | Trách nhiệm |
| --- | --- | --- |
| Application | `port/inbox/InboxEventReceipt`, `InboxEventStore` | Contract receipt dùng chung |
| Application | `ConsumeWithdrawalEventUseCase` / `impl/ConsumeWithdrawalEventUseCaseImpl` | Lock, dedup, trạng thái và gọi process, hoàn tất receipt |
| Application | `exception/InvalidWithdrawalEventException` | Phân loại event invalid cho transport |
| Application | `WithdrawalProcessingPolicy` | Chọn nguồn khởi phát |
| Application | `ProcessWithdrawalUseCaseImpl` | Bổ sung commit PROCESSING trước provider; giữ checkpoint/keys |
| Application | `ReconcileWithdrawalUseCaseImpl` | Chọn query recovery khi mode Kafka |
| Domain | `WithdrawalRepository` | Bổ sung `findRecoveryIds` |
| Infrastructure | `InboxEventEntity`, `InboxEventMapper`, `SpringDataInboxEventRepository`, `InboxEventStoreImpl` | Lưu bằng JPA, unique receipt và mapping |
| Infrastructure | `V18__create_inbox_event.sql` | Bảng inbox dùng chung và index |
| Infrastructure | `messaging/withdrawal/WithdrawalProcessKafkaListener` | Parse/validate và gọi application |
| Infrastructure | `config/WithdrawalKafkaConsumerConfiguration`, `WithdrawalKafkaConsumerProperties` | Factory riêng, retry, DLT và validation cấu hình |
| Infrastructure | `config/WithdrawalProcessingConfiguration` | Bind policy từ mode |
| Infrastructure | `SpringDataWithdrawalRepository`, `WithdrawalRepositoryImpl` | Query recovery và adapter |
| Start | `application.yaml` | Topic DLT, consumer retry và processing mode |

## Kiểm thử

- Application: replay, receipt failure, temporary failure, terminal backlog, reused event identity và chọn scheduler policy.
- PostgreSQL riêng: unique receipt theo consumer, rollback, lỗi receipt sau khi payout thành công, legacy recovery query và timeout giữ tiền rồi reconciliation hoàn tất.
- Kafka + PostgreSQL thật: create → outbox publisher → listener → process → receipt → offset; restart; duplicate và terminal backlog; hai consumer nhận cùng event ở hai partition; DLT publish failure không advance offset; retry exhaustion giữ tiền và scheduler tiếp tục đối soát.
- Provider và Redis lock port là deterministic test doubles trong integration fixture; không gọi Stripe thật. Existing Redis/Stripe adapter tests tiếp tục chạy trong full suite.

```powershell
mvn -f backend/pom.xml '-Dkafka.test.bootstrap-servers=localhost:9092' '-Doutbox.test.jdbc=jdbc:postgresql://localhost:55439/postgres?user=outbox_test' '-Dwithdrawal.test.jdbc=jdbc:postgresql://localhost:55439/postgres?user=outbox_test' clean test
```

Phải chuẩn bị PostgreSQL kiểm thử riêng. V18 mới chỉ được chạy trong schema kiểm thử, chưa migrate database ứng dụng. Các topic/group test dùng tên riêng và topic được dọn sau test. Code vẫn chưa commit.

Nguồn đối chiếu: [Spring Kafka — Handling Exceptions, DefaultErrorHandler và dead-letter recovery](https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html).
