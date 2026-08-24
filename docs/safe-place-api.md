# 안전 장소(SafePlace) API 명세서

카카오맵에서 지도를 클릭해 얻은 좌표에 주소/상세주소를 붙여 저장하고, 이름이나 주소로 검색해 좌표를 다시 꺼내 쓰기 위한 API입니다.
[안전구역(SafeZone) API](./safe-zone-api.md)와는 다른 리소스입니다 — SafeZone은 "좌표+반경(원)"만 가진 이탈 판정용이고,
SafePlace는 "좌표+주소+상세주소"를 가진 장소 저장용입니다. 등록해둔 장소를 검색해서 좌표를 얻은 뒤,
그 좌표를 안전구역 생성 시 중심좌표로 그대로 넣는 흐름을 염두에 두고 설계했습니다.

- Base URL (로컬): `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html` (SafePlace 태그)
- 모든 요청/응답은 `application/json` (UTF-8)

---

## 1. `X-Device-Id` 헤더 (SafeZone과 동일)

로그인이 없어 "이 장소가 누구 것인지"를 브라우저가 만든 UUID로 구분합니다. **안전 장소 API 5개 전부**에
`X-Device-Id: <UUID>` 헤더를 붙여야 합니다. 헤더가 없거나 빈 값이면 `400`을 응답합니다.
자세한 내용/예시 코드는 [safe-zone-api.md 1장](./safe-zone-api.md#1-먼저-알아야-할-것-x-device-id-헤더)을 그대로 재사용하세요
(같은 `getDeviceId()`/`api()` 유틸을 공유하면 됩니다).

CORS는 `http://localhost:5173`이 허용되어 있습니다.

---

## 2. 엔드포인트 요약

| 메서드 | 경로 | 설명 | 성공 코드 |
|---|---|---|---|
| POST | `/api/safe-places` | 안전 장소 등록 | 201 |
| GET | `/api/safe-places` | 내 안전 장소 목록/검색 | 200 |
| GET | `/api/safe-places/{id}` | 단건 조회 | 200 |
| PUT | `/api/safe-places/{id}` | 수정 | 200 |
| DELETE | `/api/safe-places/{id}` | 삭제 | 204 |

---

## 3. 데이터 모델

### 안전 장소 객체 (응답)

```json
{
  "id": 1,
  "name": "우리집",
  "address": "대전광역시 서구 둔산동 1420",
  "detailAddress": "101동 202호",
  "lat": 36.3504,
  "lon": 127.3845,
  "createdAt": "2026-08-24T14:22:26.809546",
  "updatedAt": "2026-08-24T14:22:26.809546"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | number | 장소 고유 번호 |
| `name` | string | 장소 이름 (예: "우리집") |
| `address` | string | 카카오맵에서 클릭/검색해 얻은 주소 (지번 또는 도로명) |
| `detailAddress` | string \| null | 사용자가 직접 입력한 상세주소 (동/호수 등). 없으면 `null` |
| `lat` / `lon` | number | 클릭 지점의 위도/경도 |
| `createdAt` / `updatedAt` | string | 생성/수정 시각 (KST) |

### 요청 본문 (POST / PUT 공통)

```json
{
  "name": "우리집",
  "address": "대전광역시 서구 둔산동 1420",
  "detailAddress": "101동 202호",
  "lat": 36.3504,
  "lon": 127.3845
}
```

| 필드 | 필수 | 제약 | 위반 시 |
|---|---|---|---|
| `name` | ✗ | 50자 이하. 생략/공백이면 `"내 안전 장소"`로 저장 | 400 |
| `address` | ✓ | 공백 불가, 255자 이하 | 400 |
| `detailAddress` | ✗ | 100자 이하. 공백만 보내면 `null`로 저장 | 400 |
| `lat` | ✓ | -90 ~ 90 | 400 |
| `lon` | ✓ | -180 ~ 180 | 400 |

> **한 기기당 최대 30개**까지 만들 수 있고, 초과하면 `409`를 응답합니다.

---

## 4. 프론트 연동 가이드 (카카오맵 등록 흐름)

### 4.1 등록 흐름

```
지도 클릭 (카카오맵 JS SDK의 click 이벤트)
  └ 클릭 좌표(lat, lon) 확보
  └ 카카오 좌표->주소 변환(geocoder.coord2Address) 또는 카카오 주소검색으로 address 확보
사용자가 상세주소 입력 (건물동/호수 등, 선택)
  └ POST /api/safe-places 로 저장
```

```js
// 카카오맵 클릭 예시 (Kakao Maps JS SDK)
kakao.maps.event.addListener(map, 'click', function (mouseEvent) {
  const latlng = mouseEvent.latLng;
  const geocoder = new kakao.maps.services.Geocoder();

  geocoder.coord2Address(latlng.getLng(), latlng.getLat(), (result, status) => {
    if (status === kakao.maps.services.Status.OK) {
      const address = result[0].road_address
        ? result[0].road_address.address_name
        : result[0].address.address_name;

      // address, latlng.getLat(), latlng.getLng()를 폼 상태에 저장해두고
      // 사용자가 이름/상세주소까지 입력한 뒤 등록 버튼을 누르면 POST 호출
    }
  });
});

async function registerSafePlace({ name, address, detailAddress, lat, lon }) {
  const res = await api('/api/safe-places', {
    method: 'POST',
    body: JSON.stringify({ name, address, detailAddress, lat, lon }),
  });
  return res.json(); // 등록된 안전 장소 객체
}
```

### 4.2 검색 — `GET /api/safe-places?query=검색어`

이름 또는 주소에 검색어가 포함된 장소만 반환합니다 (대소문자 구분 없음). `query`를 생략하면 전체 목록을
id 오름차순으로 반환합니다. **응답의 각 항목에 이미 `lat`/`lon`이 들어있으므로, 검색 결과에서 바로 좌표를
꺼내 쓰면 됩니다** (예: 안전구역 생성 폼의 초기 중심좌표로 채우기).

```http
GET /api/safe-places?query=우리집
X-Device-Id: 9f1c2f0e-6a1b-4c2e-9f1e-2b7d3a8c5e10
```

```js
async function searchSafePlaces(query) {
  const res = await api(`/api/safe-places?query=${encodeURIComponent(query)}`);
  return res.json(); // SafePlace 객체 배열. 각 항목의 lat/lon을 바로 사용 가능
}
```

### 4.3 안전구역 초기설정과 연결하기

"안전구역 초기설정" 화면에서 지도를 직접 클릭해 중심좌표를 잡을 수도 있지만, 이미 등록해둔 안전 장소를
검색해서 고르면 그 장소의 `lat`/`lon`을 그대로 [`POST /api/safe-zones`](./safe-zone-api.md#41-안전구역-생성--post-apisafe-zones)의
`centerLat`/`centerLon`으로 넘기면 됩니다. 두 리소스는 서로 참조 관계가 없는 별개의 테이블이라, 안전 장소를
지워도 이미 만들어진 안전구역에는 영향이 없습니다 (생성 시점에 좌표값만 복사되어 들어감).

```js
// 검색 결과에서 장소를 고른 뒤
const place = (await searchSafePlaces('우리집'))[0];

await api('/api/safe-zones', {
  method: 'POST',
  body: JSON.stringify({
    name: place.name,
    centerLat: place.lat,
    centerLon: place.lon,
    radiusM: 100, // 사용자가 지도에서 원 크기를 조절한 값 (10~10000)
  }),
});
```

> **30~100m 이탈 알림**은 이번 범위에 포함하지 않습니다. SafeZone의 [`GET /api/safe-zones/check`](./safe-zone-api.md#46-이탈-판정--get-apisafe-zonescheck)가
> 이미 이탈 판정 계산을 제공하므로, 나중에 알림을 붙일 때는 그 응답의 `inside`/`outsideByM`을 그대로 활용하면 됩니다.

---

## 5. 에러 응답

[safe-zone-api.md 5장](./safe-zone-api.md#5-에러-응답)과 형식이 동일합니다 (`errors[].defaultMessage` 우선, 없으면 `message`).

| 상태 코드 | 언제 |
|---|---|
| `400` | `X-Device-Id` 헤더 누락/빈 값, 또는 `address`/`lat`/`lon` 검증 위반 |
| `404` | 없는 id **또는 다른 기기의 장소** (구분 없이 404 — 이유는 SafeZone과 동일) |
| `409` | 기기당 장소 30개 초과 |

---

## 6. curl 예시

```bash
DEV="9f1c2f0e-6a1b-4c2e-9f1e-2b7d3a8c5e10"

# 등록
curl -X POST "http://localhost:8080/api/safe-places" \
  -H "X-Device-Id: $DEV" -H "Content-Type: application/json" \
  -d '{"name":"우리집","address":"대전광역시 서구 둔산동 1420","detailAddress":"101동 202호","lat":36.3504,"lon":127.3845}'

# 목록
curl "http://localhost:8080/api/safe-places" -H "X-Device-Id: $DEV"

# 검색 (이름/주소에 "둔산" 포함된 장소)
curl "http://localhost:8080/api/safe-places?query=둔산" -H "X-Device-Id: $DEV"

# 수정
curl -X PUT "http://localhost:8080/api/safe-places/1" \
  -H "X-Device-Id: $DEV" -H "Content-Type: application/json" \
  -d '{"name":"우리집","address":"대전광역시 서구 둔산동 1420","detailAddress":"102동 303호","lat":36.3504,"lon":127.3845}'

# 삭제
curl -X DELETE "http://localhost:8080/api/safe-places/1" -H "X-Device-Id: $DEV"
```

로컬 서버 실행:
```bash
cd demo
./gradlew bootRun --args='--spring.profiles.active=local'
```
