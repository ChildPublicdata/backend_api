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

import java.time.LocalDate;
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

    @Column(name = "phone_number")
    private String phoneNumber;

    // DB에는 문자열("PARENT"/"CHILD")로 저장. ORDINAL(숫자)로 저장하면 enum 순서가 바뀔 때 기존 데이터가 조용히 틀어지므로 STRING을 씀
    @Enumerated(EnumType.STRING)
    private Role role;

    // 선택 입력(부모/자녀 모두 null 가능). 자녀 화면에 생일/나이를 보여주는 용도라 부모 계정은 보통 비어있음
    @Column(name = "birth_date")
    private LocalDate birthDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected User() {
    }

    public User(String email, String passwordHash, String name, String phoneNumber, Role role, LocalDate birthDate) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.birthDate = birthDate;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Role getRole() {
        return role;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
