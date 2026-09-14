-- [이 파일이 왜 필요한가]
-- 위험구역/격자 데이터가 v4(dbscan_risk_zones_v4.json, xgboost_grid_risk_v4_partial.json)로 교체됨.
-- v4의 핵심 변화는 "미리 다듬어진 안내문(guide)"을 등급별로 다르게 준비했다는 점:
--   - 위험구역(79개) 전부와 격자 1~2급/5급은 이미 다듬어진 문구가 채워져 있고(source="llm"/"rule")
--   - 격자 3급(302개)은 아직 "template_pending"(안 다듬어진 placeholder)만 있음.
-- AiExplainService는 이제 guide_source를 보고, 이미 다듬어진 것이면 LLM을 호출하지 않고 그대로 쓰고,
-- template_pending이면 그때만 실시간으로 LLM을 호출하도록 바뀜. 그래서 이 값을 저장할 컬럼이 필요함.
--
-- [규칙] 이미 서버에 적용된 마이그레이션(V1~V13)은 절대 수정하지 말 것. 새 번호(V14)로 파일을 추가함.

-- 격자는 guide_parent/guide_child가 이미 있어서 source만 추가
alter table grid_risk add column guide_source varchar(50);

-- 위험구역은 v3까지 guide 자체가 없었으므로 세 컬럼을 통째로 추가
alter table risk_zone add column guide_parent text;
alter table risk_zone add column guide_child text;
alter table risk_zone add column guide_source varchar(50);

-- [왜 기존 데이터를 지우나] HazardDataLoader는 "테이블에 행이 있으면 적재를 건너뛴다"는 규칙이라,
-- 여기서 비워주지 않으면 이미 v3 데이터가 들어있는 환경(로컬/Railway)에서 v4가 영원히 반영되지 않음.
-- 둘 다 전부 파일에서 다시 만들어지는 파생 데이터라 사용자가 입력한 값이 섞여 있지 않음.
delete from grid_risk;
delete from risk_zone;
