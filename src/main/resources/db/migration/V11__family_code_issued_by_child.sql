-- [이 파일이 왜 필요한가]
-- 프론트 흐름이 확정되면서 연동 코드의 발급/입력 주체가 처음 설계(V6)와 반대로 정해짐:
-- "부모가 코드를 발급하고 자녀가 입력한다"가 아니라 "자녀 화면에서 코드가 생성되고, 부모가 그 코드를 입력해 연동한다".
--
-- family_link_code.parent_id는 "이 코드를 발급한 회원"을 가리키는 컬럼이었는데, 이제 그 발급 주체가
-- 항상 자녀이므로 컬럼명을 child_id로 바꿔서 실제 의미와 이름이 맞도록 함
-- (컬럼명이 parent_id인 채로 자녀의 id를 저장하면 나중에 코드를 읽는 사람이 반드시 헷갈림).
--
-- [family_link 테이블은 그대로 둠] 코드 입력이 끝난 뒤 실제로 맺어지는 연동 관계(family_link)는
-- 여전히 "부모 1명 + 자녀 1명" 쌍이라 parent_id/child_id 구조가 그대로 맞음. 바뀌는 건 코드 발급 주체뿐임.
--
-- [규칙] 이미 서버에 적용된 마이그레이션(V1~V10)은 절대 수정하지 말 것. 새 번호(V11)로 파일을 추가함.

alter table family_link_code rename column parent_id to child_id;
alter index idx_family_link_code_parent_id rename to idx_family_link_code_child_id;
