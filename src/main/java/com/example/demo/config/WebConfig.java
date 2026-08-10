// [이 파일이 왜 필요한가]
// 프론트엔드(브라우저)가 다른 주소(포트)에서 이 백엔드 API를 호출할 수 있도록 CORS를 허용해주는 설정 파일.
// 이 파일이 없으면 브라우저가 보안상 요청을 막아서, API는 정상이어도 프론트 화면에서는 데이터를 못 받아옴.
package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value; // application.yml의 설정값을 필드에 바로 꽂아주는 어노테이션
import org.springframework.context.annotation.Configuration; // 이 클래스가 "설정용 클래스"임을 스프링에 알림
import org.springframework.web.servlet.config.annotation.CorsRegistry;       // CORS 허용 규칙을 등록하는 대상
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;   // 스프링 MVC 동작(CORS 포함)을 커스터마이징하는 콜백 인터페이스

/*
 * [왜 CORS 설정이 필요한가]
 * 브라우저는 기본적으로 "다른 출처(도메인/포트)"로의 API 요청을 보안상 막는데(Same-Origin Policy),
 * 프론트엔드(예: localhost:5173)에서 이 백엔드(예: localhost:8080)로 호출하려면 서버가 명시적으로 허용해줘야 함.
 * 이 설정이 없으면 브라우저 콘솔에 CORS 에러가 뜨면서 API 응답을 프론트가 못 받아봄
 * (참고: Postman/curl 같은 도구는 브라우저가 아니라서 CORS 제약이 없어 이 설정 없이도 동작함 -> 헷갈리기 쉬운 부분).
 */
@Configuration // @Component의 특수한 버전. "이 클래스는 빈(bean) 설정을 담당한다"는 의미로 붙임
public class WebConfig implements WebMvcConfigurer {

    // application.yml의 app.cors.allowed-origins 값을 이 필드에 자동으로 주입 (콤마로 여러 개 구분 가능)
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Override // WebMvcConfigurer 인터페이스의 메서드를 재정의해서 CORS 규칙을 커스터마이징함
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // "/api/"로 시작하는 모든 요청에 대해
                .allowedOrigins(allowedOrigins.split(",")) // 위에서 설정한 origin(프론트 주소)만 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
                // [왜 명시했나] 안전구역 API는 X-Device-Id라는 표준에 없는 헤더를 받음.
                // 브라우저는 이런 커스텀 헤더가 붙은 요청을 보내기 전에 OPTIONS(preflight)로 "이 헤더 써도 되냐"를 먼저 묻는데,
                // 서버가 허용 목록에 넣어주지 않으면 본 요청이 아예 발사되지 않고 CORS 에러가 남.
                // (스프링 기본값도 "*"라 지금은 없어도 동작하지만, 나중에 누가 이 설정을 좁힐 때 실수하지 않도록 적어둠)
                .allowedHeaders("*");
    }
}
