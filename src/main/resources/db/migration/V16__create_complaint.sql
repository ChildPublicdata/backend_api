-- [이 파일이 왜 필요한가]
-- 학부모 민원(위험 지점 신고) 기능을 위한 테이블. 좌표 + 글 + 사진(선택)을 한 건으로 저장함.
-- 사진은 별도 파일 저장소(S3 등) 없이 bytea로 DB에 직접 저장함 - Railway 배포 컨테이너는
-- 재배포/재시작 때 로컬 디스크가 초기화되므로 파일시스템에 저장하면 사진이 사라짐.
create table complaint (
    id bigserial primary key,
    parent_id bigint not null,
    lat double precision not null,
    lon double precision not null,
    content text not null,
    photo bytea,
    photo_content_type varchar(100),
    created_at timestamp not null
);

-- "내 민원 목록" 조회(parent_id로 좁혀서 최신순 정렬)가 가장 흔한 조회 패턴이라 인덱스를 걸어둠
create index idx_complaint_parent_id on complaint (parent_id, created_at desc);
