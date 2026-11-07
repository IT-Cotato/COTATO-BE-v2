-- 수동 적용 전 전용 백업 및 기존 상벌점 합계 대조가 필요하다. 자동 실행되는 마이그레이션이 아니다.
-- 기존 출석·세션·뒷풀이 테이블의 행을 갱신하거나 복사하지 않는다.
CREATE TABLE manual_bonus_point (
    manual_bonus_point_id BIGINT NOT NULL AUTO_INCREMENT,
    generation_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    points INT NOT NULL,
    reason VARCHAR(300) NOT NULL,
    request_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_by BIGINT NOT NULL,
    created_by_name VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    modified_at DATETIME(6) NOT NULL,
    cancelled_at DATETIME(6) NULL,
    cancelled_by BIGINT NULL,
    cancelled_by_name VARCHAR(255) NULL,
    cancellation_reason VARCHAR(300) NULL,
    PRIMARY KEY (manual_bonus_point_id),
    CONSTRAINT uk_manual_bonus_request UNIQUE (created_by, request_id),
    CONSTRAINT ck_manual_bonus_points CHECK (points BETWEEN 1 AND 10),
    INDEX manual_bonus_generation_member_idx (generation_id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
