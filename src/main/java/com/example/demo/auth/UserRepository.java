// [이 파일이 왜 필요한가]
// 회원 테이블에 대한 DB 조회/저장 창구. 로그인/회원가입은 항상 이메일로 회원을 찾으므로
// 이메일 기준 조회 메서드가 핵심임.
package com.example.demo.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 대소문자만 다른 이메일("A@B.com" vs "a@b.com")도 같은 회원으로 취급하기 위해 IgnoreCase 사용.
    // (V6 마이그레이션의 idx_app_user_email이 lower(email)에 걸려 있어 이 조회가 인덱스를 탐)
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
