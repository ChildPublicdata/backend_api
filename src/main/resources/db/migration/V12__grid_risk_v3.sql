-- [이 파일이 왜 필요한가]
-- 격자 위험도 데이터가 v3(db_grid_risk_v3.json, 819건)로 교체되면서 JSON의 모양 자체가 바뀜.
-- v3는 "AI 점수로 등급을 매기던 방식"을 버리고 2023~2025년 3년 누적 실측(EPDO)으로 등급을 매기며,
-- 대신 XGBoost가 왜 그렇게 예측했는지를 SHAP 기여도로 함께 내려줌. 그래서 테이블도 같이 바꿔야 함.
--
-- [규칙] 이미 서버에 적용된 마이그레이션(V1~V11)은 절대 수정하지 말 것. 새 번호(V12)로 파일을 추가함.
--
-- [사라진 컬럼] v3 JSON에는 context(지역/도로명/도로형태/주요사고유형)가 아예 없음.
-- 값을 채울 소스가 없는 컬럼을 남겨두면 항상 null인 채로 API에 노출되어 오해를 부르므로 제거함.
-- 이 값을 쓰던 AI 설명 프롬프트는 대신 reasons/locationInfo/SHAP 기여도를 근거로 쓰도록 바꿨음.
alter table grid_risk drop column district;
alter table grid_risk drop column road_name;
alter table grid_risk drop column road_type;
alter table grid_risk drop column top_accident_type;

-- [등급 관련] v3는 level(1~5)과 levelName("3급 관찰"), color를 직접 내려줌.
-- 기존 grade 컬럼(WATCH/CAUTION 같은 코드)은 프론트가 이미 쓰고 있어 유지하고,
-- 적재 시 level에서 코드를 파생시켜 계속 채움 (1=DANGER, 2=CAUTION, 3=WATCH, 4=NORMAL, 5=SAFE).
alter table grid_risk add column level integer;
alter table grid_risk add column level_name varchar(255);
alter table grid_risk add column color varchar(255);

-- [사고 건수] v3는 3년 누적과 2025년 단년을 분리해서 줌. 기존 accident_count/fatalities/epdo 컬럼은
-- 3년 누적 값(total3yr/fatal3yr/epdo3yr)을 그대로 이어받고, 2025년 값만 컬럼을 새로 추가함.
alter table grid_risk add column accidents_2025 integer;
alter table grid_risk add column fatal_2025 integer;

-- [설명 근거] 문자열 배열과 SHAP 기여도 목록이라 컬럼으로 펼치지 않고 jsonb로 통째 저장함.
-- 개수가 격자마다 다르고(0~3개) 서버는 이 값을 검색 조건으로 쓰지 않고 그대로 내려주기만 하기 때문.
alter table grid_risk add column reasons jsonb;
alter table grid_risk add column location_info jsonb;
alter table grid_risk add column shap_positive jsonb;
alter table grid_risk add column shap_negative jsonb;

-- [보호자/아이용 안내 문구] v3에서 사고 이력이 있는 격자(449건)에만 채워져 있고 나머지는 null임.
alter table grid_risk add column guide_parent text;
alter table grid_risk add column guide_child text;

-- 모든 격자에 동일하게 붙는 모델 한계 고지 문구. 프론트가 상세 화면 하단에 그대로 노출하는 용도.
alter table grid_risk add column model_note text;

-- [왜 기존 데이터를 지우나] HazardDataLoader는 "테이블에 행이 있으면 적재를 건너뛴다"는 규칙이라,
-- 여기서 비워주지 않으면 이미 v2 846건이 들어있는 환경(로컬/Railway)에서 v3가 영원히 반영되지 않음.
-- 격자는 전부 파일에서 다시 만들어지는 파생 데이터라 사용자가 입력한 값이 섞여 있지 않음.
delete from grid_risk;
