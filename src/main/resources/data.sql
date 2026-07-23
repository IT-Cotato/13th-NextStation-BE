-- 약관 title/content가 아직 확정 전이라 재시작마다 전체 삭제 후 재삽입한다.
-- TODO: 회원가입 완료 API 구현 후 member_terms_agreement가 이 테이블을 FK로 참조하기 시작하면 이 DELETE는 제약 위반으로 실패한다.
-- TODO: 그 시점엔 버전 관리(ON DUPLICATE KEY UPDATE 등) 방식으로 전환 예정

DELETE FROM terms_consents;

INSERT INTO terms_consents (title, content, version, is_required, created_at, updated_at)
VALUES ('서비스 이용약관', '제1조 (목적) ...', 'v1.0', true, NOW(), NOW());

INSERT INTO terms_consents (title, content, version, is_required, created_at, updated_at)
VALUES ('개인정보 수집 및 이용 동의', '제1조 (수집 항목) ...', 'v1.0', true, NOW(), NOW());

INSERT INTO terms_consents (title, content, version, is_required, created_at, updated_at)
VALUES ('마케팅 정보 수신 동의', '제1조 (수신 목적) ...', 'v1.0', false, NOW(), NOW());

-- Category/PlaceTag 마스터 데이터.
-- 둘 다 place가 자연키(code/name)로 참조하므로 PlaceSeeder보다 먼저 시딩돼야 한다.
-- default_image_url은 사진 업로드(S3) 단계 전이라 아직 비워둔다.
-- place.category_id/place_tag_mapping이 이 테이블들을 FK로 참조하므로 재시작마다 DELETE하면 제약 위반으로 실패한다.
-- 그래서 DELETE 없이 자연키(code/name) 기준 upsert로 멱등하게 반영한다.

INSERT INTO category (code, name, default_image_url)
VALUES ('CAFE', '카페', NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name), default_image_url = VALUES(default_image_url);

INSERT INTO category (code, name, default_image_url)
VALUES ('FOOD', '식당', NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name), default_image_url = VALUES(default_image_url);

INSERT INTO category (code, name, default_image_url)
VALUES ('CULTURE', '문화공간', NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name), default_image_url = VALUES(default_image_url);

INSERT INTO category (code, name, default_image_url)
VALUES ('WALK', '산책포인트', NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name), default_image_url = VALUES(default_image_url);

INSERT INTO place_tag (name, is_active) VALUES ('NATURE', true) ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);
INSERT INTO place_tag (name, is_active) VALUES ('ALLEY_TRIP', true) ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);
INSERT INTO place_tag (name, is_active) VALUES ('MARKET', true) ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);
INSERT INTO place_tag (name, is_active) VALUES ('HOTPLACE', true) ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);
INSERT INTO place_tag (name, is_active) VALUES ('PHOTO_SPOT', true) ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);
INSERT INTO place_tag (name, is_active) VALUES ('SHOPPING', true) ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);
INSERT INTO place_tag (name, is_active) VALUES ('EXPERIENCE', true) ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);
INSERT INTO place_tag (name, is_active) VALUES ('BUDGET', true) ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);
INSERT INTO place_tag (name, is_active) VALUES ('INDOOR', true) ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);
