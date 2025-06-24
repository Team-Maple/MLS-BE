# Maple Land

## 개발 환경 설정 및 실행 가이드

### 1. PostgreSQL Docker 컨테이너 실행

```bash
docker run --name mysql \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=maple_db \
  -e MYSQL_USER=maple_username \
  -e MYSQL_PASSWORD=maple_password \
  -p 3306:3306 \
  -d mysql:8
```
혹은
```bash
create database maple_db;
create user 'maple_username'@'%' IDENTIFIED BY 'maple_password';
grant all privileges on maple_db.* to 'maple_username'@'%';
flush privileges;
```
****
## 2. 실행 스크립트
```bash
$ ./gradlew bootRun
```

### 2-1. 로컬환경 스크립트 ( JPA DDL Auto Update 설정 )
```
$ ./gradlew bootRun --args='--spring.profiles.active=local'
```

### 2-1. 로컬환경 스크립트 ( JPA DDL Auto Validate 설정 )
```
$ ./gradlew bootRun --args='--spring.profiles.active=live'
```

## 3. Spring Docs UI 경로
```plaintext
http://localhost:8080/swagger-ui-maple/index.html
```