-- 홈 사진 설정만 추가한다. 기존 회원·기수·출석·점수 데이터는 수정하지 않는다.
-- 로컬 검토용 변경이며 현재 운영 DB에는 실행하지 않는다.
CREATE TABLE homepage_image (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slot VARCHAR(40) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    desktop_x DOUBLE NOT NULL,
    desktop_y DOUBLE NOT NULL,
    desktop_zoom DOUBLE NOT NULL,
    mobile_x DOUBLE NOT NULL,
    mobile_y DOUBLE NOT NULL,
    mobile_zoom DOUBLE NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_homepage_image_slot UNIQUE (slot),
    CONSTRAINT ck_homepage_image_desktop CHECK (
        desktop_x BETWEEN 0 AND 100 AND desktop_y BETWEEN 0 AND 100 AND desktop_zoom BETWEEN 1 AND 3
    ),
    CONSTRAINT ck_homepage_image_mobile CHECK (
        mobile_x BETWEEN 0 AND 100 AND mobile_y BETWEEN 0 AND 100 AND mobile_zoom BETWEEN 1 AND 3
    )
);
