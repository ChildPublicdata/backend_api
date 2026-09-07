-- [이 파일이 왜 필요한가]
-- V7__create_map_icon.sql로 만든 map_icon(좌표+아이콘번호 공개 마커)은 원래 의도를 잘못 구현한 것으로
-- 확인됨: 실제로 필요했던 건 "안전 장소(SafePlace) 등록 화면에서 이름/주소와 함께 아이콘(학교/병원/집/책)도
-- 같이 고르는 것"이었음. 즉 아이콘은 익명 공개 마커가 아니라 SafePlace 한 건에 딸린 속성이어야 함.
--
-- 그래서 이번 마이그레이션은 두 가지를 함께 함:
-- 1) safe_place에 icon_type(1=학교, 2=병원, 3=집, 4=책) 컬럼 추가
-- 2) 잘못 만든 map_icon 테이블 제거 (아직 프론트에서 쓰인 적 없는 기능이라 데이터 손실 우려 없음)
--
-- [규칙] 이미 서버에 적용된 마이그레이션(V1~V8)은 절대 수정하지 말 것. 새 번호(V9)로 파일을 추가함.
--
-- [기존 safe_place 행은 어떻게 되나] not null + default 1(학교)을 줘서, 이미 등록된 안전 장소가 있는
-- 배포 환경에서도 컬럼 추가가 실패하지 않고 임시로 "학교" 아이콘이 채워짐. 실제 값은 프론트에서
-- 수정(PUT) API로 다시 저장할 때 바로잡히면 됨.
alter table safe_place add column icon_type integer not null default 1;

drop table map_icon;
