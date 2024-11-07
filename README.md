# Reactive example

## 서버 실행

```bash
$ ./gradlew bootRun
```

## 클라이언트 실행

14번 째 줄 path를 요청할 path로 변경합니다.
```
$ node app.js
```

## POST /response/mono/void

응답이 `Mono<Void>`인 경우

## POST /response/flow

응답이 `Flow<String>`인 경우

## POST /request/flow

요청이 `Flow<User>`이고 응답이 `Flow<User`인 경우 응답 `Content-type`은 `application/json`이다.

## POST /request/flow/xnd

요청이 `Flow<User>`이고 응답이 `Flow<User`인 경우 응답 `Content-type`은 `application/x-ndjson`이다.
