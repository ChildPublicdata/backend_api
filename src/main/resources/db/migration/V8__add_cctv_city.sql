-- [이 파일이 왜 필요한가]
-- CCTV 데이터를 "대전서구 단독"에서 "대전서구 + 경기도 안양시"로 넓히면서 Cctv 엔티티에
-- city(관할 지자체) 컬럼을 추가했음. ddl-auto=validate라 Hibernate가 테이블을 직접 안 만들어주므로
-- 이 컬럼은 Flyway 마이그레이션으로 직접 추가해야 함.
--
-- 그리고 V2__reseed_safety_bell.sql과 같은 이유로, DataSeeder는 cctv 테이블에 이미 행이 있으면
-- (기존 대전서구 812건) 적재를 건너뛰기 때문에 city 컬럼도, 새로 추가된 안양시 1749건도 영영 안 들어감.
-- 그래서 컬럼 추가와 함께 기존 행을 비워서 DataSeeder가 새 cctv.json(대전서구+안양시, city 포함)을
-- 통째로 다시 적재하게 함.
alter table cctv add column city varchar(255);

delete from cctv;
