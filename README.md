# Kafka 

## 기본구조
![image](https://user-images.githubusercontent.com/103620466/230803794-afb44bed-b9ab-42c6-96e2-5a895580bb30.png)   
1. Kafka Cluster : 메세지를 저장하는 저장소
    - 하나의 Cluster는 여러 개의 Broker(=Server)로 구성
    - Broker들은 메세지를 나눠서 저장, 이중화 처리, 장애 대체 등의 역할 수행
2. Zookeeper Cluster : Kafka Cluster를 관리
    - Kafka Cluster와 관련된 정보를 기록 및 관리 역할 수행
3. Producer : Kafka Cluster에 메세지를 넣는 역할 수행
4. Consumer : Kafka Cluster에 메세지를 읽는 역할 수행

## Topic & Partition
### Topic
- 메세지를 저장하는 단위, 메세지를 구분하는 용도로 사용
- 파일시스템의 폴더와 유사

### Partition
- 메세지를 저장하는 물리적인 파일
- 한 개의 Topic은 한 개 이상의 Partition으로 구성
- Partition은 추가만 가능한 파일이기 때문에 Producer가 넣은 메세지는 Partition의 맨 뒤에 추가됨
- Consumer는 Offset 기준으로 메세지를 순서대로 읽음 → 한 Partition 내에서만 메세지 순서를 보장함   
  (Offset :  각 메세지가 저장되는 위치)
- Partition에 저장된 메세지는 Consumer가 읽은지에 대한 여부에 상관 없이 삭제되지 않음
- Round-Robin 또는 Key로 메세지의 Partition 선택
- 한 개의 Partition은 Consumer-Group(Consumer가 속한 Group)의 한 개의 Consumer만 연결 가능
![image](https://user-images.githubusercontent.com/103620466/230806109-2d876b33-cb6b-43da-accf-c8e03902dbd7.png)
```
  → 즉, Consumer-Group에 속한 Consumer들은 한 Partition을 공유할 수 없음
  → 한 Consumer-Group 기준으로 Partition의 메세지는 순서대로 처리됨
  → 단, 한 Partition을 공유할 수 없는 것은 한 Consumer-Group 내에서만 불가능하며 다른 Group은 가능함
```

## 성능
1. OS Page-Cache 사용
    - 파일 IO를 메모리에서 처리하기 때문에 빠름
2. Zero Copy
    - 디스크 버퍼에서 네트워크 버퍼로 직접 복사
3. 단순한 Broker 역할
    - 일반적으로 메세지 필터, 재전송을 Broker에서 실행하지만 Kafka는 Producer와 Consumer가 직접 수행
    - Broker는 Consumer와 Partition 간의 Mapping 관리
4. Batch(묶어서 보내기/받기)   
    → 낱개 처리보다 처리량 증가
    - Producer : 일정 크기만큼 메세지를 모아서 전송
    - Consumer : 최소 크기만큼 메세지를 모아서 조회
5. 처리량 증대의 편의성
    - 장비 용량의 한계 → Broker, Partition 추가
    - Consumer 느림 → Consumer(Partition) 추가   
    → 수평 확장에 용이
6. 장애대응
    - Replica(=Partition 복제)
      - 복제수(Replication factor) 만큼 Partition의 복제본을 각 Broker에 생성
      - 본 Broker를 리더, 복제된 Broker를 팔로워라 칭함
      - Producer와 Consumer는 리더를 통해서만 메세지를 처리하며 팔로워는 리더로부터 복제만 수행
      - 리더의 장애 발생 시 다른 팔로워가 리더를 대체함
      
## Producer
Topic에 메세지 전송 방식
```
Properties prop = new Properties(); // Producer 설정 정보
prop.put("bootstrap.servers", "kafka01:9092,kafka01:9092,kafka01:9092"); // kafka host, server
prop.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
prop.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

KafkaProducer<Integer, String> producer = new KafkaProducer<>(prop);

// message 전송 (ProducerRecord : Broker에 전송할 메시지)
producer.send(new ProducerRecord<>("topicname", "key", "value"));
producer.send(new ProducerRecord<>("topicname", "value"));

producer.close();
```

### Producer 기본 흐름
![image](https://user-images.githubusercontent.com/103620466/230809375-b25eebc1-f55b-46a4-9a73-72056caed23c.png)   
1. send() 메소드를 사용하여 레코드 전달
2. Serializer를 통한 Byte 배열 변환
3. Partitioner를 통하여 메세지를 어느 Topic의 Partition으로 보낼지 결정
4. Batch에 묶어 Buffer에 저장
5. Sender가 Batch에서 차례대로 메세지를 가져와 Kafka Broker로 전송
    - Sender는 별도의 Thread로 동작
    - 그렇기 때문에 배치에 메세지가 다 찼는지 여부에 상관없이 차례대로 Broker로 전송
    - send() 메소드도 Sender와 상관없이 배치에 메세지를 누적
   
### 처리량 관련 주요 속성
- Batch Size : Batch의 최대 크기, 해당 크기 만큼 배치가 다 차면 전송
   - Size가 작은 경우 : 한번에 보낼 수 있는 메세지의 양이 줄기 때문에 전송 횟수가 증가하고 처리량이 감소함
- linger.ms : 전송 대기 시간
  - 기본값 : 0
  - 대기 시간이 없는 경우 배치를 바로 전송하며 있는 경우 그 시간만큼 대기 후 배치 전송
  - 대기 시간이 높은 경우 : 대기 시간이 존재하나 배치에 메세지를 모아서 전송하기 때문에 처리량이 증가함
  
### 전송결과
- 별도의 처리 없이는 전송 실패 여부를 확인할 방법이 없음

#### Futrue
```
Future<RecordMetadata> f = producer.send(new ProducerRecord<>("topic", "value"));
try {
    RecordMetadata meta = f.get(); //블로킹
} catch(ExecutionException ex) {

}
```
- Futrue의 get 사용 시 해당 시점에서 블로킹 수행
  - 메세지 처리를 단건으로 진행 → Batch에 메세지가 쌓이지 않음
  - Batch의 효과가 떨어져 처리량 감소
- 처리량이 낮아도 메세지 전송이 확실히 진행되었는 확인이 필요한 경우에만 사용

#### Callback
```
producer.send(new ProducerRecord<>("topic", "value"),new Callback() {
  @Override
  public void onCompletion(RecordMetadata metadata, Exception ex) {
    //Exception을 받을 경우 전송 실패
  }
})
```
- onCompletion 메소드에서 Exception을 받는 경우에 따라 전송 성공/실패 여부 판단
- 처리량에 저하 없음

### 전송보장
#### ack : 전송보장을 위한 설정
- ack = 0
  - 서버 응답을 기다리지 않음 → 전송 보장이 되지 않음
  - 처리량은 높아지나 메세지가 유실되는 경우 전송 여부를 확인할 수 없음
- ack = 1
  - Partition 리더에 저장되면 응답
  - 단, 리더 장애 발생 시 메세지 유실 발생
- ack = all(-1)
  - 엄격하게 전송을 보장해야하는 경우 사용
  - 모든 Replica에 저장되면 응답 (Replica의 개수는 Broker의 min.insync.replicas 설정에 따름)

#### min.insync.replicas
- Producer ack 옵션이 all인 경우 저장에 성공했다고 응답할 수 있는 동기화된 Replica의 최소 개수
```
ex) Replica 3개, ack = all, min.insync.replicas = 2 
    → 리더에 저장하고 팔로워 중 한 개에 저장되면 성공 응답
    
ex) Replica 3개, ack = all, min.insync.replicas = 1
    → 리더에 저장되면 성공 응답
    → ack = 1과 동일하여 리더 장애 시 메세지 유실 가능
    
ex) Replica 3개, ack = all, min.insync.replicas = 3
    → 리더와 팔로우 모두 저장되야 성공 응답
    → 팔로워 중 한 개라도 장애 발생 시 성공 응답을 못하기 때문에 일반적으로 min.insync.replicas을 Replica 개수와 동일하게 하지 않음
```

## 에러
### 에러 유형
- 전송 전 실패
    - 직렬화 실패
    - Producer 자체 요청 크기 제한 초과
    - 최대 대기 시간 초과 등
- 전송 과정 실패
    - 전송 타임 아웃
    - 일시적인 네트워크 오류
    - 리더 다운에 의한 새 리더 선출 진행
    - Broker 설정 메세지 크기 한도 초과 등

### 에러 대응
1. 재시도 : Broker 응답 타임 아웃, 일시적 리더 부재 등과 같은 재시도 가능한 에러는 재시도 처리
    - Producer 자체적으로 Broker 전송 과정에서 에러 발생 시 재시도 가능한 에러에 대하 재전송 시도(retries 속성)
    - send() 메소드에서 Exception 발생 시 타입에 따라 send() 재호출
    - Callback 메소드에서 Exception 타입에 따라 send() 재호출   

2. 기록 : 추후 처리를 위해 별도의 파일, DB 등에 실패 메세지 기록
    - send() 메소드 Exception 발생 시
    - send() 메소드에 전달한 Callback에서 Exception 발생 시
    - send() 메소드가 리턴한 Futrue의 get() 메소드에서 Exception 발생 시

<br>

🚩 주의
1. 특별한 이유 없이 무한 재시도 금지
    - 재시도를 할 경우 다음에 보내야할 메세지를 지연시키는 것과 같기 때문에 재시도 횟수나 시간을 정할 것
2. 재시도에 따른 메세지 중복 전송 가능
    - Broker의 응답이 늦게와 재시도할 경우 중복 발송 가능(enabled.idempotence 속성)
3. 재시도 순서
    - max.in.flight.requests.per.connection : Blocking 없이 한 Connection에서 전송할 수 있는 최대 전송 요청 수
        - 해당 값이 1보다 클 경우 재시도 시점에 따라 메세지 순서가 바뀔 수 있음
        - 따라서, 전송 순서가 중요한 경우 해당 값을 1로 고정
        ![image](https://user-images.githubusercontent.com/103620466/230825342-86d0e49b-8d05-40d5-896c-13baf2b6750a.png)

## Consumer
Topic Partition에서 레코드 조회
```
Properties prop = new Properties();
prop.put("bootstrap.servers", "localhost:9092");
prop.put("group.id", "group1"); //컨슈머 그룹
prop.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"); //역직렬화 설정
prop.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(prop);
consumer.subscribe(Collections.singleton("simple")); //토픽 구독

while (조건) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMailis(100));
    for (ConsumerRecord<String, String> record : records) {
        System.out.println(record.value() + ":" + record.topic() + ":" + record.partition() + ":" + record.offset());
    }
}   

consumer.close();
```

- Topic Partition 그룹 단위 할당
    - Consumer-Group 단위로 Partition 할당
    - 이 때, Partition 개수보다 Consumer-Group이 많아지게 되면 잉여 Consumer 발생   
        → Partition 파트의 "한 개의 Partition은 Consumer-Group의 한 개의 Consumer만 연결 가능"의 개념과 연결   
    ![image](https://user-images.githubusercontent.com/103620466/230827357-84956289-c212-4fa9-93fa-fff690e5cf14.png)   
    - Consumer 개수가 Partition 개수보다 커지면 안되며 만약, 처리량을 위해 Consumer를 늘려야 한다면 Partition의 개수도 같이 늘려야함

### Commit & Offset
#### poll()
1. Consumer poll() 메소드는 이전에 Commit한 오프셋이 존재하면 그 이후의 레코드를 읽어옴
2. 읽어온 오프셋의 마지막 레코드를 Commit
3. 다시 poll() 메소드가 실행되면 앞서 Commit된 오프셋 이후의 레코드를 읽어오는 과정을 반복

#### Commit된 Offset이 없는 경우
- 처음 접근이거나 Commit한 오프셋이 없는 경우
- auto.offset.reset 설정
    - earliset : 맨 처음 오프셋 사용
    - latest : 가장 마지막 오프셋 사용(기본값)
    - none : 이전 Commit이 없으면 Exception 발생 (보통은 사용하지 않음)

### Consumer Setting
#### 조회에 영향을 주는 주요 설정
1. fetch.min.bytes : 조회 시 Broker가 전송할 최소 데이터 크기 (Broker는 해당 설정값 이상의 데이터가 쌓일 때까지 대기)
    - 기본값 : 1
    - 해당 값이 크면 대기 시간은 늘지만 처리량 증가

2. fetch.max.wait.ms : 데이터가 최소 크기가 될 때까지 기다리는 시간
    - 기본값 : 500ms
    - Broker가 리턴할 때까지 대기하는 시간 != Poll 메소드의 대기 시간

3. max.partition.fetch.bytes : Partition당 Server(=Broker)가 리턴할 수 있는 최대 크기
    - 기본값 : 1MB

#### Auto Commit
- enable.auto.commit
    - true : 일정 주기로 Consumer가 읽은 오프셋 Commit(기본값)
    - false : 수동으로 Commit 실행

- auto.commit.interval.ms : 자동 Commit 주기
    - enable.auto.commit가 true인 경우 일정 주기를 설정하는 옵션
    - 기본값 : 5000ms
    - poll(), close() 메소드 호출 시 자동 Commit 실행

#### Manual Commit
- 동기처리
```
ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
for (ConsumerRecords<String, String> record : records) {
    // 처리
}
try {
    consumer.commitSync();
} catch (Exception ex) {
    // 커밋 실패 시 에러 발생
}
```

- 비동기처리
```
ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
for (ConsumerRecords<String, String> record : records) {
    // 처리
}
consumer.commitAsync();
// 성공여부 확인 필요 시 -> commitAsync(OffsetCommitCallback callback)
```

#### 재처리
- 일시적인 커밋 실패, 리밸런스 등에 의해 동일한 메세지 조회 가능
- 따라서, Consumer는 멱등성(idempotence)을 고려
     - 멱등성 : 동일한 요청을 한 번 보내는 것과 여러 번 연속으로 보내는 것이 같은 효과를 지니고, 서버의 상태도 동일하게 남을 때, 해당 HTTP 메서드가 멱등성을 가졌다고 말함
```
ex) 조회수 1 증가 → 좋아요 1 증가 → 조회수 1 증가 : 재처리하는 경우
단순 처리 시 조회수가 2가 아닌 4가 증가할 수 있음
```
- 그렇기 때문에 Consumer는 데이터 특성에 따라 TimeStamp 또는 일련번호 등을 활용하여 데이터를 두 번이상 처리해도 이상 없도록 고려

#### Consumer-Group 유지 방안
- Session Time-out & Heartbeat 
    - Consumer는 Heartbeat를 Broker에 전송해 연결 유지
        - Broker는 일정 시간 Consumer로부터 Heartbeat이 없다면 그룹에서 제외하고 리밸런스 진행
        - session.timeout.ms : 세션타임 아웃시간 (기본값 : 10초)
        - heartbeat.intervla.ms : 하트비트 전송주기 (기본값 : 3초)
        ```
        heartbeat.intervla.ms는 session.timeout.ms의 1/3 이하로 설정하도록 권장
        ```
- max.poll.interval.ms
    - poll() 메소드의 최대 호출 간격
    - 해당 설정 시간이 지나도록 poll이 실행되지 않으면 그룹에서 제외하고 리밸런스 진행

#### 종료처리
- 무한 roop를 돌며 poll() 메소드를 통해 레코드를 읽어올 때 roop를 벗어날 때 사용
- 다른 Tread에서 wakeup() 메소드 호출
- 이때, poll() 메소드는 WakeupException을 발생시키고 이 Exception을 while roop 외부에서 catch
- catch 후 consumer의 close() 메소드 호출
```
KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(prop);
consumer.subscribe(Collections.singleton("simple"));

try {
    while (true) {
        // wakeup() 호출 시 Exception 발생.
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

        // records 처리..
        for (ConsumerRecord<String, String> record : records) {
            System.out.println(record.value() + ":" + record.topic() + ":" + record.partition() + ":" + record.offset());
        }

        try {
            consumer.commitAsync();  
        } catch (Ecception e) {
            e.printStackTrace();
        }
    }   
} catch (Exception ex) {
    //..
} finally {
    consumer.close();
}
```

## 주의사항
- Kafka Consumer는 Tread에 안전하지 않음
    - 여러 Tread에서 동시 사용 금지
    - 단, wakeup() 메소드는 예외
   

## 참조
https://data-make.tistory.com/731   
https://developer.mozilla.org/ko/docs/Glossary/Idempotent   
https://www.youtube.com/@madvirus

