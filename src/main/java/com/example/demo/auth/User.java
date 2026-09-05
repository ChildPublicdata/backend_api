// [이 파일이 왜 필요한가]
// 회원 한 명(부모 또는 자녀)이 DB에 어떤 모양으로 저장될지 정의하는 파일. app_user 테이블과 매핑됨.
package com.example.demo.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// V6 마이그레이션이 테이블명을 user가 아닌 app_user로 만들었으므로(user는 Postgres 예약어) 명시적으로 매핑
@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    // DB 컬럼명을 password_hash로 못박음 (기본 변환 규칙대로면 passwordhash가 되어 SafeZone의 radiusM과 같은 문제가 남)
    @Column(name = "password_hash")
    private String passwordHash;

    private String name;

    // DB에는 문자열("PARENT"/"CHILD")로 저장. ORDINAL(숫자)로 저장하면 enum 순서가 바뀔 때 기존 데이터가 조용히 틀어지므로 STRING을 씀
    @Enumerated(EnumType.STRING)
    private Role role;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected User() {
    }

    public User(String email, String passwordHash, String name, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
