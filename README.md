# 로컬 개발 환경 세팅

## 구성

| 서비스 | 접속 주소 | 설명 |
|--------|-----------|------|
| PostgreSQL | localhost:5432 | 메인 DB |
| pgAdmin | http://localhost:5050 | DB GUI (admin@admin.com / admin) |
| MinIO S3 API | http://localhost:9000 | 파일 스토리지 API |
| MinIO Console | http://localhost:9001 | 파일 스토리지 GUI (minioadmin / minioadmin123) |

## 시작

```bash
# docker 디렉토리로 이동
cd docker

# 컨테이너 시작 (백그라운드)
docker compose up -d

# 로그 확인
docker compose logs -f

# 상태 확인
docker compose ps
```

## 중지 / 초기화

```bash
# 컨테이너 중지
docker compose down

# 볼륨까지 삭제 (DB 초기화)
docker compose down -v
```

## pgAdmin DB 연결 설정

1. http://localhost:5050 접속
2. Servers → Register → Server
3. General 탭: Name = association
4. Connection 탭:
   - Host: postgres (컨테이너명)
   - Port: 5432
   - Database: association_db
   - Username: association_user
   - Password: association_pass

## Spring Boot application.yml 설정

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/association_db
    username: association_user
    password: association_pass
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true

cloud:
  aws:
    s3:
      endpoint: http://localhost:9000
      bucket: association-files
    credentials:
      access-key: minioadmin
      secret-key: minioadmin123
    region:
      static: us-east-1
```
