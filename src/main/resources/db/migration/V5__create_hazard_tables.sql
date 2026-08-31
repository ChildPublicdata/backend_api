-- [이 파일이 왜 필요한가]
-- 위험구역(risk_zone)/격자 위험도(grid_risk)/시설(facility) 3개 테이블을 만드는 SQL.
-- 이 프로젝트가 PostGIS를 쓸 수 있는 postgis-db 서비스로 옮겨간 뒤 처음 추가하는 마이그레이션이라,
-- 여기서 PostGIS 확장을 켜고 geometry 컬럼 + GIST 공간 인덱스까지 함께 만듦.
--
-- [규칙] 이미 서버에 적용된 마이그레이션(V1~V4)은 절대 수정하지 말 것. 새 번호(V5)로 파일을 추가함.

-- geometry 타입과 ST_MakeEnvelope/ST_Intersects/ST_DWithin 같은 함수를 쓰려면 이 확장이 먼저 켜져 있어야 함.
-- IF NOT EXISTS: 이미 켜져 있으면 조용히 넘어가서, 이 파일이 여러 환경에서 실행돼도 안전함
create extension if not exists postgis;

-- 사고 이력을 DBSCAN으로 군집화해서 뽑은 위험구역 (79건)
create table risk_zone (
    zone_id           varchar(255) not null,
    type              varchar(255),
    center            geometry(Point, 4326),
    radius_m          integer,
    risk_score        integer,
    grade             varchar(255),
    epdo              double precision,
    accidents         integer,
    fatalities        integer,
    serious           integer,
    minor             integer,
    district          varchar(255),
    road_name         varchar(255),
    road_type         varchar(255),
    top_accident_type varchar(255),
    primary key (zone_id)
);

-- [GIST 인덱스] geometry 컬럼 전용 공간 인덱스. bounding box 조회(ST_Intersects)와
-- 반경 조회(ST_DWithin)가 이 인덱스를 타야 전체 행을 훑지 않고 빠르게 좁혀짐
create index idx_risk_zone_center on risk_zone using gist (center);

-- 안양시 전역을 250m 간격으로 덮는 격자별 AI 예측 위험도 (846건)
create table grid_risk (
    grid_id                     varchar(255) not null,
    center                      geometry(Point, 4326),
    size_m                      integer,
    risk_score                  integer,
    grade                       varchar(255),
    has_accident                boolean,
    accident_count              integer,
    epdo                        double precision,
    fatalities                  integer,
    cctv_dist_m                 integer,
    cctv_count_200m             integer,
    school_zone_dist_m          integer,
    in_school_zone              boolean,
    intersection_accidents_300m integer,
    district                    varchar(255),
    road_name                   varchar(255),
    road_type                   varchar(255),
    top_accident_type           varchar(255),
    primary key (grid_id)
);

create index idx_grid_risk_center on grid_risk using gist (center);
-- GET /api/grids?minRisk= 조회가 매번 risk_score 이상 필터를 거는데, 공간 인덱스만으로는
-- 이 조건을 좁힐 수 없어서 일반 B-tree 인덱스를 별도로 하나 더 둠
create index idx_grid_risk_score on grid_risk (risk_score);

-- CCTV 및 어린이보호구역 (1402건)
create table facility (
    facility_id  varchar(255) not null,
    type         varchar(255),
    purpose      varchar(255),
    location     geometry(Point, 4326),
    name         varchar(255),
    camera_count integer,
    has_cctv     boolean,
    radius_m     integer,
    address      varchar(255),
    primary key (facility_id)
);

create index idx_facility_location on facility using gist (location);
-- GET /api/facilities?type= 필터와 /api/safety의 "타입별 최근접 시설" 조회가 항상 type으로 좁히기 때문에 인덱싱
create index idx_facility_type on facility (type);
