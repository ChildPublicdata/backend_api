package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/*
 * [@ActiveProfiles("local")이 왜 필요한가]
 * @SpringBootTest는 앱을 실제로 한 번 띄워보는 테스트라 DB 연결이 필요함.
 * 이게 없으면 기본 설정(application.yml)으로 뜨면서 Postgres 접속정보(PGHOST 등)를 찾다가 실패함.
 * 내 PC에도, GitHub/Railway의 빌드 서버에도 Postgres가 없으니 테스트가 항상 깨지게 됨.
 * local 프로파일을 켜면 설치가 필요 없는 인메모리 DB(H2)로 뜨기 때문에 어디서든 통과함.
 *
 * [이 테스트가 실제로 검증하는 것]
 * "contextLoads"라 이름은 단순하지만, 앱이 뜰 때 하는 일을 전부 통과해야 성공하는 테스트임:
 *   - Flyway가 db/migration/V1__init.sql을 실제로 실행 (SQL 오타가 있으면 여기서 실패)
 *   - Hibernate가 엔티티 구조와 만들어진 테이블이 일치하는지 검사 (ddl-auto: validate)
 *   - DataSeeder가 JSON 3종을 읽어 DB에 적재 (JSON 형식이 DTO와 안 맞으면 여기서 실패)
 * 그래서 데이터 파일이나 엔티티를 건드린 뒤 이 테스트만 돌려봐도 큰 실수는 대부분 잡힘.
 */
@SpringBootTest
@ActiveProfiles("local")
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
