# Kafka local — bước 1

Mục tiêu: chạy một Kafka broker và xác nhận broker nhận kết nối. Chưa tạo topic, chưa viết producer/consumer và chưa nối withdrawal. Không cần chạy database hay backend.

## Chạy từ thư mục gốc dự án

Docker Desktop phải chạy Linux containers. Kiểm tra Docker Engine:

```powershell
docker version
docker compose -f environment/docker-compose.yml config --quiet
docker compose -f environment/docker-compose.yml up -d --wait --wait-timeout 180 kafka
docker compose -f environment/docker-compose.yml ps kafka
```

Lần đầu Docker tải image apache/kafka:4.1.2. Khi thành công, service kafka có trạng thái healthy.

## Các cấu hình cần hiểu

| Cấu hình | Ý nghĩa |
|---|---|
| image | Image chính thức Apache Kafka, ghim phiên bản để dễ tái hiện |
| process.roles=broker,controller | Một node vừa lưu message vừa quản lý metadata cluster |
| KRaft | Kafka quản lý metadata bằng Raft, không cần ZooKeeper |
| node.id=1 | Định danh node, khác với cluster ID |
| CLUSTER_ID | Định danh cluster; giữ nguyên khi dùng lại volume dữ liệu |
| controller.quorum.voters | Node controller số 1 ở kafka:9093 |
| listeners | Các địa chỉ/cổng broker lắng nghe trong container |
| advertised.listeners | Địa chỉ Kafka trả lại cho client trong metadata |
| offsets.topic.replication.factor=1 | Topic nội bộ lưu offset dùng một bản sao vì chỉ có một broker |
| transaction.state.log.*=1 | Cho phép topic transaction nội bộ hoạt động với một broker |
| auto.create.topics.enable=false | Topic sẽ được tạo rõ ràng ở bước 2 |
| kafka_data | Named volume giữ dữ liệu khi container được tạo lại |
| healthcheck | Thử gọi Kafka protocol; kiểm tra kỹ hơn việc chỉ mở cổng TCP |

Kafka không phải HTTP server: mở localhost:9092 bằng trình duyệt không phải cách kiểm tra broker.

## Các địa chỉ kết nối

```text
Ứng dụng chạy trên Windows → localhost:9092 → EXTERNAL listener
Container cùng mạng Compose → kafka:19092 → INTERNAL listener
Controller nội bộ          → kafka:9093 → không publish ra host
```

Kafka nằm trong Compose chính và cùng digital-wallet-network với backend. Khi tích hợp Spring Kafka ở bước sau, backend trong Docker sẽ dùng kafka:19092; backend chạy trên Windows sẽ dùng localhost:9092. Hiện backend chưa được cấu hình kết nối Kafka. PLAINTEXT và một node là thiết lập thực hành local, chưa có authentication/TLS hoặc khả năng chịu lỗi nhiều broker.

## Kiểm tra và xem log

```powershell
docker compose -f environment/docker-compose.yml exec kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server kafka:19092
docker compose -f environment/docker-compose.yml logs --tail 80 kafka
```

Lệnh API versions phải trả danh sách Kafka API được broker hỗ trợ. Healthcheck chỉ kiểm tra listener nội bộ; kết nối producer/consumer từ host sẽ được kiểm tra ở bước kết nối ứng dụng.

## Dừng hoặc chạy tiếp

```powershell
docker compose -f environment/docker-compose.yml stop kafka
docker compose -f environment/docker-compose.yml start kafka
```

Chỉ định service kafka trong lệnh để điều khiển riêng Kafka. Lệnh stop không xóa dữ liệu trong named volume. Stack chính dùng volume digital-wallet_kafka_data; volume của lần chạy Compose riêng trước đó được giữ lại, không tự động sao chép hoặc xóa. Nếu cổng 9092 đang được dùng, cần đổi cả host port mapping và cổng EXTERNAL trong advertised.listeners tương ứng.

Tham khảo: https://kafka.apache.org/41/getting-started/docker/ và ví dụ single-node trong repository apache/kafka, tag 4.1.2.
## Bước 2 — kết nối Spring Kafka vào backend

Bước này thêm kết nối và topic thật của withdrawal, chưa thêm publisher, consumer listener hoặc outbox. Luồng giữ tiền và scheduler hiện tại vẫn hoạt động như trước.

### Các file thay đổi

| File | Vai trò |
|---|---|
| backend/digitalwallet-infrastructure/pom.xml | Thêm spring-boot-starter-kafka 4.1.0 theo dòng Spring Boot của dự án |
| infrastructure/config/KafkaBusinessProperties.java | Bind cấu hình spring.kafka.topics/groups/provisioning và kiểm tra topic/partition/replica hợp lệ |
| infrastructure/config/KafkaTopicsConfiguration.java | Khai báo KafkaAdmin.NewTopics từ danh mục topic; KafkaAdmin do Boot cung cấp tạo topic lúc khởi động |
| backend/digitalwallet-start/src/main/resources/application.yaml | Địa chỉ broker, producer/consumer settings, group ID và thông số topic |
| environment/docker-compose.yml | Backend dùng kafka:19092 và chờ Kafka healthy khi chạy bằng Compose |
| infrastructure/config/KafkaTopicsConfigurationTest.java | Kiểm thử mặc định, override và dữ liệu cấu hình không hợp lệ |
| infrastructure/config/KafkaConnectionTest.java | Khởi động riêng phần Kafka bằng YAML thật, kiểm tra cấu hình và metadata broker |

Các đường dẫn infrastructure/config ở bảng trên thuộc package com.tyson.digitalwallet.ddd.infrastructure.config, trong src/main/java hoặc src/test/java tương ứng.

### Những giá trị đang dùng

| Cấu hình | Giá trị / ý nghĩa |
|---|---|
| spring.kafka.bootstrap-servers | localhost:9092 khi chạy trên máy; Compose truyền SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:19092 |
| spring.kafka.topics.withdrawal-process.name | wallet.withdrawal.process.v1 |
| spring.kafka.topics.withdrawal-process.partitions | 3; có thể đổi bằng WITHDRAWAL_KAFKA_PARTITIONS |
| spring.kafka.topics.withdrawal-process.replication-factor | 1 cho một broker local |
| spring.kafka.groups.withdrawal-worker | wallet-withdrawal-workers; đổi bằng WITHDRAWAL_KAFKA_GROUP_ID |
| producer key/value serializer | StringSerializer; message contract sẽ được triển khai ở bước publisher/consumer |
| producer acks | all (Kafka biểu diễn nội bộ là -1) |
| producer enable.idempotence | true; chống một số trường hợp retry trùng ở producer, không thay idempotency nghiệp vụ |
| consumer enable-auto-commit | false |
| listener ack-mode | record; khi có listener, container commit tiến độ sau khi listener xử lý thành công, không phải do timer auto-commit |
| consumer auto-offset-reset | earliest; chỉ áp dụng khi group không có offset hợp lệ |
| admin fail-fast | true; backend cần Kafka sẵn sàng khi khởi động để tạo/kiểm tra topic |

Chưa có listener nên consumer group chưa được đăng ký hoạt động trên broker. KafkaTemplate và listener factory được chuẩn bị bởi auto-configuration; chưa có message withdrawal nào được gửi.

### Kiểm chứng kết nối mà không chạy database

Chạy từ thư mục gốc dự án:

```powershell
docker compose -f environment/docker-compose.yml up -d --wait kafka
mvn -f backend/pom.xml -pl digitalwallet-infrastructure -am '-Dtest=KafkaTopicsConfigurationTest,KafkaConnectionTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dkafka.test.bootstrap-servers=localhost:9092' test
```

Connection test chỉ khởi động KafkaAutoConfiguration và KafkaTopicsConfiguration, không chạy toàn bộ ứng dụng, Flyway, Redis hay Stripe. Nó dùng KafkaAdmin qua kết nối thật để tạo topic nếu chưa có và đọc lại partition/replica. Khi không truyền bootstrap server test, connection test được skip để bộ unit test không phụ thuộc Docker.

Có thể xem topic sau test:

```powershell
docker compose -f environment/docker-compose.yml exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:19092 --describe --topic wallet.withdrawal.process.v1
```

Kết quả mong đợi: PartitionCount: 3, ReplicationFactor: 1. Chỉ có metadata/topic được tạo; chưa gửi message rút tiền.

Tham khảo Spring: https://docs.spring.io/spring-kafka/reference/kafka/configuring-topics.html và https://docs.spring.io/spring-boot/reference/messaging/kafka.html
## Tổ chức nhiều nghiệp vụ dưới spring.kafka

Các thuộc tính bootstrap-servers, admin, producer, consumer và listener là cấu hình chuẩn do Spring Boot xử lý. topics, groups và provisioning là phần mở rộng của dự án, được KafkaBusinessProperties bind riêng với ignoreUnknownFields=true. Cả hai properties bean cùng tồn tại dưới prefix spring.kafka; không thay thế KafkaProperties của Boot.

```yaml
spring:
  kafka:
    provisioning:
      enabled: true
    topics:
      withdrawal-process:
        name: wallet.withdrawal.process.v1
        partitions: 3
        replication-factor: 1
    groups:
      withdrawal-worker: wallet-withdrawal-workers
```

Thêm nghiệp vụ mới bằng cách thêm một alias trong topics và group xử lý trong groups. Cấu hình mặc định chỉ khai báo nghiệp vụ withdrawal hiện có. Topic/group bổ sung trong unit test chỉ là dữ liệu kiểm thử. Connection test tạo thêm một topic tạm, xác nhận cả hai topic đều được provision từ danh mục rồi xóa topic tạm.

Khi spring.kafka.provisioning.enabled=false (hoặc không có thuộc tính này), KafkaTopicsConfiguration không tạo bean NewTopics. Properties vẫn được bind để publisher/consumer sử dụng topic/group có sẵn. Việc tắt provisioning không xóa topic và không tắt Kafka client. application.yaml mặc định bật provisioning cho môi trường local; có thể truyền KAFKA_TOPIC_PROVISIONING_ENABLED=false khi deployment tự quản lý topic.

Không dùng consumer.group-id toàn cục cho mọi nghiệp vụ. Listener ở bước sau phải chọn group riêng, ví dụ:

```java
@KafkaListener(
    topics = "${spring.kafka.topics.withdrawal-process.name}",
    groupId = "${spring.kafka.groups.withdrawal-worker}"
)
```

Ví dụ trên chưa được thêm vào code runtime; bước hiện tại chỉ cấu hình và kiểm thử binding/provisioning, chưa xử lý message hay thay đổi flow tiền.
## Bước 3: outbox dùng chung

Đã bổ sung bảng outbox_event và ghi event cùng transaction tạo withdrawal. Chưa có publisher/consumer; scheduler hiện tại vẫn hoạt động. Xem [thiết kế và flow outbox](../docs/architecture/outbox.md).

## Bước 4: Outbox Publisher

Publisher dùng chung đã đọc outbox qua JPA và gửi Kafka. Mặc định bật trong application.yaml; đặt OUTBOX_PUBLISHER_ENABLED=false để tắt. Retry có backoff, mỗi hàng giữ row lock trong lúc chờ ACK; scheduler outbox có thread riêng. Chưa có consumer runtime và scheduler withdrawal vẫn hoạt động. Xem [flow, config và test publisher](../docs/architecture/outbox.md#publisher-đã-triển-khai).

## Bước 5: Withdrawal Consumer

Consumer đã nhận event và gọi process use case, chống trùng bằng inbox_event cùng các khóa/idempotency hiện có. Mặc định WITHDRAWAL_PROCESSING_MODE=kafka; scheduler chỉ đối soát payout đã bắt đầu. Đổi scheduler để dùng luồng cũ và không đăng ký listener. Topic DLT mặc định wallet.withdrawal.process.v1.dlt; hết retry không tự hoàn tiền. Xem [file, flow, retry và chuyển giao](../docs/architecture/withdrawal-kafka.md). Chưa migrate database ứng dụng, chưa commit.
