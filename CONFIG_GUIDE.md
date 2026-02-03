A:

1. IDE를 재시작하거나 Gradle을 다시 빌드하세요
2. `spring.profiles.active`가 제대로 설정되었는지 확인하세요
3. 로그에서 "The following profiles are active: xxx" 메시지를 확인하세요

### Q: local 환경에서 OAuth 로그인이 안 돼요

A: `application-local.yml`의 OAuth Client ID/Secret 값을 실제 값으로 변경하세요

### Q: prod 환경변수를 어디에 설정해야 하나요?

A:

- Docker: `docker-compose.yml`의 `environment` 섹션
- Kubernetes: ConfigMap 또는 Secret
- AWS: Elastic Beanstalk 환경 변수 또는 Parameter Store
- 로컬 테스트: `.env` 파일 (Git에 커밋하지 마세요!)

## 참고 자료

- [Spring Boot Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
