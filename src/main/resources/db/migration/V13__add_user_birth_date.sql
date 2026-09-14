-- [이 파일이 왜 필요한가]
-- 자녀 화면에 생일/나이를 보여줄 수 있도록 회원(app_user)에 생년월일 컬럼을 추가함.
-- 부모/자녀 모두 선택 입력이라 not null 제약을 걸지 않고, 기존 회원은 전부 null로 채워짐.
alter table app_user add column birth_date date;
