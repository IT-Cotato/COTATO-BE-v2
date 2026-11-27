-- 로컬 검토용. 앱 기동이나 배포 도구에서 자동 실행하지 않는다.
-- 홈페이지 표시 일정만 확장한다. 사진·크롭·revision과 기존 도메인 데이터는 보존한다.
ALTER TABLE homepage_image
    MODIFY COLUMN object_key VARCHAR(512) NULL,
    ADD COLUMN display_schedule VARCHAR(80) NULL,
    ADD COLUMN display_title VARCHAR(80) NULL,
    ADD COLUMN display_description VARCHAR(2000) NULL;
-- 프로젝트 타입이 VARCHAR이면 STUDY 추가를 위한 DDL은 필요 없다.
-- ENUM인 환경은 SHOW COLUMNS FROM project LIKE 'project_type'로 기존 값을 먼저 확인한 후
-- 기존 ENUM 값을 모두 유지하며 STUDY를 추가해야 한다. 운영에 임의 실행하지 않는다.
