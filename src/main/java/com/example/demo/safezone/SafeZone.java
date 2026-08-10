// [이 파일이 왜 필요한가]
// 안전구역 1건이 DB에 어떤 모양(컬럼 구조)으로 저장될지 정의하는 파일.
// SafetyBell/Cctv 엔티티와 역할은 같지만, 저 둘은 공공데이터를 "넣고 읽기만" 하는 반면
// 이 엔티티는 사용자가 만든 뒤 수정/삭제까지 하기 때문에 값을 바꾸는 메서드(update)가 추가로 있음.
package com.example.demo.safezone;

import jakarta.persistence.Column;         // 자바 필드와 DB 컬럼의 연결을 직접 지정할 때 사용
import jakarta.persistence.Entity;         // 클래스를 DB 테이블과 매핑되는 대상으로 표시
import jakarta.persistence.GeneratedValue; // PK 값을 DB가 자동으로 채번하게 함
import jakarta.persistence.GenerationType; // 자동 채번 전략의 종류(IDENTITY 등)를 고르는 enum
import jakarta.persistence.Id;             // 테이블의 기본키(PK) 필드를 표시

import java.time.LocalDateTime; // 날짜+시간을 담는 자바 표준 타입 (DB의 timestamp 컬럼과 대응)

/*
 * [왜 이런 구조인가 - setter 대신 update() 메서드]
 * 필드마다 setName/setRadiusM 같은 setter를 열어두면 "아무 코드나 아무 때나 값을 바꿀 수 있는" 상태가 됨.
 * 그러면 updated_at 갱신을 깜빡하거나, 절반만 바뀐 이상한 상태가 만들어지기 쉬움.
 * 그래서 값을 바꾸는 통로를 update() 하나로 좁혀두고, 그 안에서 updatedAt까지 같이 챙김.
 *
 * [왜 deviceId는 update()에 없나]
 * 주인이 바뀌는 일은 없어야 하기 때문. 생성할 때 한 번 정해지고 그 뒤로는 못 바꾸게 막아둔 것.
 */
@Entity // 이 클래스 = safe_zone 테이블 (V3__create_safe_zone.sql에서 만든 그 테이블)
public class SafeZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB의 auto-increment에 PK 채번을 위임
    private Long id;

    private String deviceId;          // 이 구역의 주인 (브라우저가 만든 UUID). 자바 deviceId -> DB device_id 로 자동 변환됨
    private String name;              // 구역 이름
    private Double centerLat;         // 원의 중심 위도
    private Double centerLon;         // 원의 중심 경도
    // [왜 이 필드만 @Column으로 컬럼명을 직접 적었나 - 실제로 앱이 안 뜨던 문제]
    // 스프링의 기본 이름 변환 규칙은 "소문자 다음 대문자 다음 소문자"일 때만 밑줄을 넣음(centerLat -> center_lat).
    // radiusM은 대문자 M으로 "끝나서" 그 조건에 안 걸리고 그냥 전부 소문자로 붙여 radiusm이 됨.
    // 그래서 SQL의 radius_m 컬럼을 못 찾아 기동이 실패했음(Schema-validation: missing column [radiusm]).
    // 이름 변환 규칙에 기대지 않고 컬럼명을 못 박아서 해결함.
    @Column(name = "radius_m")
    private Integer radiusM;          // 원의 반지름 (미터)
    private LocalDateTime createdAt;  // 만든 시각
    private LocalDateTime updatedAt;  // 마지막으로 고친 시각

    // [왜 필요한가] JPA가 DB에서 row를 읽어 객체를 만들 때 파라미터 없는 생성자로 빈 객체를 먼저 만들기 때문.
    // 외부에서 실수로 쓰지 못하게 protected로 막아둠.
    protected SafeZone() {
    }

    // 실제로 새 안전구역을 만들 때 쓰는 생성자.
    // createdAt/updatedAt을 여기서 직접 채우기 때문에 Controller가 시각을 신경 쓸 필요가 없음.
    public SafeZone(String deviceId, String name, Double centerLat, Double centerLon, Integer radiusM) {
        this.deviceId = deviceId;
        this.name = name;
        this.centerLat = centerLat;
        this.centerLon = centerLon;
        this.radiusM = radiusM;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt; // 방금 만들었으니 "고친 시각"도 만든 시각과 동일
    }

    // 구역 수정(이름/중심/반경). 프론트에서 원을 드래그해 크기나 위치를 바꾸면 이 메서드가 호출됨.
    public void update(String name, Double centerLat, Double centerLon, Integer radiusM) {
        this.name = name;
        this.centerLat = centerLat;
        this.centerLon = centerLon;
        this.radiusM = radiusM;
        this.updatedAt = LocalDateTime.now(); // 값이 바뀌었으니 "고친 시각"을 지금으로 갱신
    }

    // 아래부터는 필드 값을 꺼내 쓰기 위한 getter (단순 반환이라 개별 설명 생략)
    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getName() {
        return name;
    }

    public Double getCenterLat() {
        return centerLat;
    }

    public Double getCenterLon() {
        return centerLon;
    }

    public Integer getRadiusM() {
        return radiusM;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
