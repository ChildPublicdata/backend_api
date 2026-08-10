# 안전구역(SafeZone) API 명세서

프론트엔드에서 지도 위에 원을 그려 "안전구역"을 저장하고, 현재 위치가 그 원을 벗어나면 알람을 띄우기 위한 API입니다.

- Base URL (로컬): `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html` (SafeZone 태그)
- 모든 요청/응답은 `application/json` (UTF-8)

---

## 1. 먼저 알아야 할 것: `X-Device-Id` 헤더

아직 로그인/회원 기능이 없어서, **"이 안전구역이 누구 것인지"를 브라우저가 만든 식별자로 구분**합니다.

- 프론트가 최초 1회 랜덤 UUID를 만들어 `localStorage`에 저장합니다.
- **안전구역 API 5개 전부**에 `X-Device-Id: <그 UUID>` 헤더를 붙여 보냅니다.
- 헤더가 없거나 빈 값이면 `400`을 응답합니다.

```js
// 프론트 공통 유틸 예시
function getDeviceId() {
  let id = localStorage.getItem('deviceId');
  if (!id) {
    id = crypto.randomUUID();          // 예: "9f1c2f0e-6a1b-4c2e-9f1e-2b7d3a8c5e10"
    localStorage.setItem('deviceId', id);
  }
  return id;
}

const api = (path, options = {}) =>
  fetch(`http://localhost:8080${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-Device-Id': getDeviceId(),
      ...options.headers,
    },
  });
```

> ⚠️ 주의
> - `deviceId`는 **본인 확인이 아니라 브라우저 구분값**입니다. 그 값을 아는 사람은 해당 구역을 조회/수정할 수 있습니다. 민감한 정보를 이름에 넣지 마세요.
> - localStorage를 지우거나 다른 브라우저·시크릿 모드로 접속하면 **다른 사람으로 인식되어 기존 구역이 안 보입니다.** (정상 동작입니다)
> - 나중에 로그인이 붙으면 이 헤더는 인증 토큰으로 대체될 예정입니다. 헤더를 붙이는 코드는 위처럼 **한 곳(api 유틸)에 모아두세요.**

CORS는 `http://localhost:5173`(Vite 기본 포트)이 허용되어 있습니다. 다른 포트를 쓴다면 백엔드의 `CORS_ALLOWED_ORIGINS` 환경변수에 추가해야 합니다.

---

## 2. 엔드포인트 요약

| 메서드 | 경로 | 설명 | 성공 코드 |
|---|---|---|---|
| POST | `/api/safe-zones` | 안전구역 생성 | 201 |
| GET | `/api/safe-zones` | 내 안전구역 목록 | 200 |
| GET | `/api/safe-zones/{id}` | 단건 조회 | 200 |
| PUT | `/api/safe-zones/{id}` | 수정 (원 이동/크기 변경) | 200 |
| DELETE | `/api/safe-zones/{id}` | 삭제 | 204 |
| GET | `/api/safe-zones/check` | **현재 위치 이탈 판정** | 200 |

---

## 3. 데이터 모델

### 안전구역 객체 (응답)

```json
{
  "id": 1,
  "name": "집 주변",
  "centerLat": 36.335,
  "centerLon": 127.372,
  "radiusM": 500,
  "createdAt": "2026-08-10T14:22:26.809546",
  "updatedAt": "2026-08-10T14:22:26.809546"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | number | 구역 고유 번호 |
| `name` | string | 구역 이름 |
| `centerLat` | number | 원 중심 위도 |
| `centerLon` | number | 원 중심 경도 |
| `radiusM` | number | 원 반지름 (**미터**) |
| `createdAt` / `updatedAt` | string | 생성/수정 시각 (`yyyy-MM-ddTHH:mm:ss.SSSSSS`, 서버 로컬시각 = KST) |

> `radiusM`은 **화면 픽셀이 아니라 실제 거리(m)** 입니다. 커서로 원을 키울 때 Leaflet이면 `L.circle`의 `radius` 값이 이미 미터 단위이므로 그대로 보내면 됩니다. (`L.circleMarker`는 픽셀 단위이므로 사용하지 마세요.)

### 요청 본문 (POST / PUT 공통)

```json
{
  "name": "집 주변",
  "centerLat": 36.335,
  "centerLon": 127.372,
  "radiusM": 500
}
```

| 필드 | 필수 | 제약 | 위반 시 |
|---|---|---|---|
| `name` | ✗ | 50자 이하. 생략/공백이면 `"내 안전구역"`으로 저장 | 400 |
| `centerLat` | ✓ | -90 ~ 90 | 400 |
| `centerLon` | ✓ | -180 ~ 180 | 400 |
| `radiusM` | ✓ | 10 ~ 10000 (정수, 미터) | 400 |

> **반경 하한이 10m인 이유**: GPS 오차가 보통 수~수십 미터라, 그보다 작은 원은 가만히 서 있어도 알람이 계속 울립니다.
> **한 기기당 최대 20개**까지 만들 수 있고, 초과하면 `409`를 응답합니다.

---

## 4. 엔드포인트 상세

### 4.1 안전구역 생성 — `POST /api/safe-zones`

**요청**
```http
POST /api/safe-zones
X-Device-Id: 9f1c2f0e-6a1b-4c2e-9f1e-2b7d3a8c5e10
Content-Type: application/json

{"name":"집 주변","centerLat":36.3350,"centerLon":127.3720,"radiusM":500}
```

**응답 `201 Created`** — [안전구역 객체](#안전구역-객체-응답)

```js
const res = await api('/api/safe-zones', {
  method: 'POST',
  body: JSON.stringify({ name, centerLat: center.lat, centerLon: center.lng, radiusM: Math.round(radius) }),
});
const zone = await res.json();
```

---

### 4.2 내 안전구역 목록 — `GET /api/safe-zones`

**응답 `200 OK`** — 안전구역 객체의 **배열** (id 오름차순). 없으면 `[]`

```json
[
  {"id":1,"name":"집 주변","centerLat":36.335,"centerLon":127.372,"radiusM":500,
   "createdAt":"2026-08-10T14:22:26.809546","updatedAt":"2026-08-10T14:22:26.809546"}
]
```

> 페이징이 없습니다. 한 기기의 구역은 최대 20개라 전부 한 번에 내려줍니다.
> 지도 진입 시 이 API를 한 번 호출해 원들을 그려두면 됩니다.

---

### 4.3 단건 조회 — `GET /api/safe-zones/{id}`

**응답 `200 OK`** — 안전구역 객체
**`404 Not Found`** — 없는 id이거나, **다른 기기의 구역**인 경우

---

### 4.4 수정 — `PUT /api/safe-zones/{id}`

원을 드래그해 위치를 옮기거나 크기를 바꿨을 때 호출합니다.

- **부분 수정이 아니라 통째로 덮어씁니다.** 반경만 바꾸더라도 `centerLat`, `centerLon`을 반드시 함께 보내야 합니다.
- 생략된 `name`은 `"내 안전구역"`으로 덮어써집니다. 이름을 유지하려면 기존 값을 그대로 실어 보내세요.

**요청 / 응답** — 본문 형식은 생성과 동일, 응답 `200 OK` (안전구역 객체, `updatedAt` 갱신됨)
**`404 Not Found`** — 없는 id이거나 다른 기기의 구역

> 원을 드래그하는 동안 매 프레임 호출하면 요청이 폭주합니다. **드래그가 끝난 시점(`dragend`, `mouseup`)에 한 번만** 호출하세요.

---

### 4.5 삭제 — `DELETE /api/safe-zones/{id}`

**응답 `204 No Content`** — 본문 없음 (`res.json()`을 호출하면 에러가 납니다)
**`404 Not Found`** — 없는 id이거나 다른 기기의 구역

---

### 4.6 이탈 판정 — `GET /api/safe-zones/check`

현재 위치가 내 안전구역 안인지 서버가 계산해서 알려줍니다. **알람의 근거가 되는 API입니다.**

**요청**
```http
GET /api/safe-zones/check?lat=36.3450&lon=127.3720
X-Device-Id: 9f1c2f0e-6a1b-4c2e-9f1e-2b7d3a8c5e10
```

| 쿼리 파라미터 | 필수 | 설명 |
|---|---|---|
| `lat` | ✓ | 현재 위치 위도 (-90 ~ 90) |
| `lon` | ✓ | 현재 위치 경도 (-180 ~ 180) |

**응답 `200 OK`**
```json
{
  "lat": 36.345,
  "lon": 127.372,
  "zoneCount": 1,
  "inside": false,
  "nearestZoneId": 1,
  "zones": [
    {
      "id": 1,
      "name": "집 주변",
      "centerLat": 36.335,
      "centerLon": 127.372,
      "radiusM": 500,
      "distanceM": 1111.9,
      "inside": false,
      "outsideByM": 611.9
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `lat`, `lon` | number | 판정에 사용한 좌표 (요청값 그대로 — 디버깅용) |
| `zoneCount` | number | 등록된 구역 개수 |
| **`inside`** | boolean | **전체 판정. 구역 중 하나라도 안에 있으면 `true`** |
| `nearestZoneId` | number \| null | 경계가 가장 가까운 구역 id. 구역이 없으면 `null` |
| `zones[]` | array | 구역별 계산 결과. **경계에 가까운 순으로 정렬** |
| `zones[].distanceM` | number | 구역 중심에서 현재 위치까지 거리 (m, 소수점 1자리) |
| `zones[].inside` | boolean | 이 구역 안에 있는지 (`distanceM <= radiusM`, 경계선 위는 안으로 봄) |
| `zones[].outsideByM` | number | 이 구역에서 벗어난 거리 (m). 안에 있으면 `0` |

**알람 조건**: `inside === false` → 알람
**구역이 0개일 때는 `inside: true`, `zones: []`** 입니다. ("지킬 구역이 없으면 이탈도 없음" — 구역을 만들지 않은 사용자에게 알람이 울리지 않게 하기 위함)

거리는 하버사인(haversine) 공식으로 계산합니다 (지구를 구로 가정, 오차 0.5% 이내).

---

## 5. 에러 응답

에러는 스프링 기본 형식으로 내려갑니다.

```json
{
  "timestamp": "2026-08-10T05:22:55.766+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for object='safeZoneRequest'. Error count: 1",
  "errors": [
    { "field": "radiusM", "defaultMessage": "반경은 10m 이상이어야 합니다" }
  ],
  "path": "/api/safe-zones"
}
```

`errors` 배열은 **본문 검증(@Valid) 실패 시에만** 포함되며, 이때 `message`는 위처럼 두루뭉술한 문장입니다.
따라서 사용자에게 보여줄 문구는 **`errors`가 있으면 `errors[].defaultMessage`, 없으면 `message`** 를 쓰세요.

```js
const showError = (body) =>
  body.errors?.length
    ? body.errors.map(e => e.defaultMessage).join('\n')
    : body.message;
```

`404`(`"SafeZone not found: 1"`), `409`(`"안전구역은 기기당 최대 20개까지 만들 수 있습니다"`), 헤더 누락(`"X-Device-Id 헤더가 필요합니다"`)은 `errors` 없이 `message`에 사유가 담깁니다.

| 상태 코드 | 언제 | 대응 |
|---|---|---|
| `400` | `X-Device-Id` 헤더 누락/빈 값 | 헤더 확인 |
| `400` | 필수값 누락, 좌표/반경 범위 위반 | `errors`의 메시지를 표시 |
| `404` | 없는 id **또는 다른 기기의 구역** | 목록을 다시 불러오기 |
| `409` | 기기당 구역 20개 초과 | "더 만들 수 없습니다" 안내 |

> `404`가 "없음"과 "남의 것"을 구분하지 않는 것은 의도된 동작입니다. 구분해서 알려주면 남의 구역이 존재한다는 사실이 노출됩니다.

---

## 6. 프론트 연동 가이드 (이탈 알람)

### 6.1 권장 구조

```
지도 진입
  └ GET /api/safe-zones          → 원 그리기
커서로 원 그리기/조절
  └ POST 또는 PUT                 → 저장 (드래그 끝난 시점 1회)
위치 추적 시작
  └ navigator.geolocation.watchPosition
      └ 로컬에서 거리 계산 → inside 여부 판단
      └ 상태가 "안 → 밖"으로 바뀐 순간에만 알람
```

### 6.2 매 위치 갱신마다 서버를 부를 필요는 없습니다

`watchPosition`은 몇 초에 한 번씩 콜백이 돕니다. 그때마다 `/check`를 호출하면 요청이 과합니다.
구역 목록(`centerLat`, `centerLon`, `radiusM`)을 이미 받아왔으므로 **거리 계산은 프론트에서 하는 편이 빠르고 오프라인에서도 동작합니다.** `/check`는 서버 기준 판정이 필요할 때(진입 시 1회, 알람 직전 재확인 등) 쓰세요.

```js
// 서버와 동일한 하버사인 공식 (미터 단위)
function distanceM(lat1, lon1, lat2, lon2) {
  const R = 6371000, rad = d => d * Math.PI / 180;
  const dLat = rad(lat2 - lat1), dLon = rad(lon2 - lon1);
  const a = Math.sin(dLat / 2) ** 2 +
            Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

const isInsideAny = (lat, lon, zones) =>
  zones.length === 0 ||
  zones.some(z => distanceM(z.centerLat, z.centerLon, lat, lon) <= z.radiusM);
```

### 6.3 알람은 "상태가 바뀐 순간"에만

```js
let wasInside = true;

navigator.geolocation.watchPosition(
  ({ coords }) => {
    const inside = isInsideAny(coords.latitude, coords.longitude, zones);
    if (wasInside && !inside) showAlarm();   // 안 → 밖: 이탈 알람
    if (!wasInside && inside) clearAlarm();  // 밖 → 안: 알람 해제
    wasInside = inside;
  },
  err => console.warn('위치 권한/오차', err),
  { enableHighAccuracy: true, maximumAge: 5000, timeout: 10000 }
);
```

매번 알람을 띄우면 경계에 서 있을 때 몇 초마다 알람이 반복됩니다. **직전 상태와 달라졌을 때만** 띄우세요.

### 6.4 경계에서 알람이 깜빡이는 문제 (권장 처리)

GPS 좌표는 가만히 있어도 수 미터씩 흔들립니다. 경계 근처에서는 `inside`가 true/false를 오가며 알람이 반복될 수 있습니다. 두 가지 중 하나를 권합니다.

- **여유 거리(hysteresis)**: 이탈 판정은 `반경 + 30m`을 넘을 때, 복귀 판정은 `반경` 안으로 들어올 때로 다르게 둡니다.
- **연속 확인**: 연속 2~3회 연속으로 밖이라고 나올 때만 알람을 띄웁니다.

추가로 `coords.accuracy`(오차 반경, 미터)가 100m를 넘는 값은 실내 등에서 나온 부정확한 값일 가능성이 높으니 판정에서 건너뛰는 것도 좋습니다.

### 6.5 그 밖에 유의할 점

- **HTTPS 필요**: `navigator.geolocation`은 `https://` 또는 `http://localhost`에서만 동작합니다.
- **권한 거부 처리**: 사용자가 위치 권한을 거부하면 콜백이 아예 안 옵니다. 안내 문구를 준비하세요.
- 브라우저 알림(`Notification`)을 쓸 경우 `Notification.requestPermission()`을 사용자의 클릭 이벤트 안에서 호출해야 합니다.

---

## 7. curl 예시 (검증 완료)

```bash
DEV="9f1c2f0e-6a1b-4c2e-9f1e-2b7d3a8c5e10"

# 생성
curl -X POST "http://localhost:8080/api/safe-zones" \
  -H "X-Device-Id: $DEV" -H "Content-Type: application/json" \
  -d '{"name":"집 주변","centerLat":36.3350,"centerLon":127.3720,"radiusM":500}'

# 목록
curl "http://localhost:8080/api/safe-zones" -H "X-Device-Id: $DEV"

# 이탈 판정 (약 1.1km 북쪽 → inside:false, outsideByM:611.9)
curl "http://localhost:8080/api/safe-zones/check?lat=36.3450&lon=127.3720" -H "X-Device-Id: $DEV"

# 수정
curl -X PUT "http://localhost:8080/api/safe-zones/1" \
  -H "X-Device-Id: $DEV" -H "Content-Type: application/json" \
  -d '{"name":"집 주변","centerLat":36.3350,"centerLon":127.3720,"radiusM":1500}'

# 삭제
curl -X DELETE "http://localhost:8080/api/safe-zones/1" -H "X-Device-Id: $DEV"
```

로컬 서버 실행:
```bash
cd demo
./gradlew bootRun --args='--spring.profiles.active=local'
```
(H2 인메모리 DB라 서버를 끄면 저장한 구역이 사라집니다. 배포 환경은 PostgreSQL이라 유지됩니다.)
