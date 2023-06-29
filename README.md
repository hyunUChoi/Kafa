# 야매 카톡 만들기

## 준비물
- kafka 다운로드  
    - kafka 공홈 주소 : https://kafka.apache.org/downloads
- Web Socket 통신 프로그램
    - 구글 WebSocket Test Client 프로그램
    - Postman
    - 등등 통신 가능한거 아무거나 가능

## 실행순서
### 1. Zookeeper 실행  
- 실행방법  
    - kafka 다운로드 및 압축풀기  
    - cmd 창 실행 후 아래 코드 입력
```
cd {압축 풀었던 kafka 폴더}
.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties
```
🚨 Window라면 .bat으로 Linux라면 .shell 파일을 사용하면 된다  
🚨 error에 '입력 줄이 너무 깁니다. 명령 구문이 올바르지 않습니다.'라 뜨면 폴더 경로가 길어서 그런거니 C드라이브 밑에 적당히 넣어주면 해결!  

### 2. kafka 실행
- 새로운 cmd 창 열고 아래 코드 입력
```
.\bin\windows\kafka-server-start.bat .\config\server.properties
```
🚨 error에 'Shutdown broker because all log dirs in ~~'라 뜨면 C:/tmp 하위에 kafka-logs 폴더와 zookeeper 폴더를 제거하면 해결!  

### 3. Topic 생성
- 또 새로운 cmd 창 열고 아래 코드 입력
```
.\bin\windows\kafka-topics.bat --create --bootstrap-server localhost:port --replication-factor 1 --partitions 1 --topic Topic이름
```
```
--create : 새로운 토픽을 만들 때 사용하는 옵션
--bootstrap-server : 연결할 Kafka 서버( host:port )
--replication-factor : Partition 복제 수
--partitions : Topic이 생성되거나 변경될 때의 Partition 수
--topic : create, alter, describe, delete 옵션에 사용할 Topic 이름, 이름은 큰따옴표(")로 묶고, 정규식 사용이 가능하므로 \로 escape
```
<br>

### 참고
- Topic 상세보기
```
.\bin\windows\kafka-topics.bat --describe --topic Topic이름 --bootstrap-server localhost:port
```
<br>

- Topic 목록
```
.\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:port
```
<br>

- Topic 삭제
```
.\bin\windows\kafka-topics.bat --delete --topic Topic이름 --bootstrap-server localhost:port
```
<br>

- Topic 도움말
```
.\bin\windows\kafka-topics.bat --help
```   
### 4. Java 실행
