# Maple Land

## 개발 환경 설정 및 실행 가이드

### 1. PostgreSQL Docker 컨테이너 실행

```bash
docker run --name maple-postgresql \
  -e POSTGRES_USER=maple_username \
  -e POSTGRES_PASSWORD=maple_password \
  -e POSTGRES_DB=maple_db \
  -p 5432:5432 \
  -d postgres:15
```

### 2. 실행 스크립트
```bash
$ ./gradlew bootRun
```

## 2-1. 로컬환경 스크립트 ( JPA DDL Auto Update 설정 )
```
$ ./gradlew bootRun --args='--spring.profiles.active=local'
```

## 2-1. 로컬환경 스크립트 ( JPA DDL Auto Validate 설정 )
```
$ ./gradlew bootRun --args='--spring.profiles.active=live'
```