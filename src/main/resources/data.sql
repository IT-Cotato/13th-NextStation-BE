-- 카테고리/장소태그 마스터 데이터
-- place/place_tag_mapping이 각각 이 테이블들을 FK로 참조하므로
-- 재시작마다 DELETE 후 재삽입하면 참조가 생긴 뒤부터는 제약 위반으로 실패한다.
-- 그래서 PK(id)를 그대로 유지한 채 내용만 갱신하는 INSERT ... ON DUPLICATE KEY UPDATE로 시딩한다.

-- Category/PlaceTag 마스터 데이터.
-- 둘 다 place가 자연키(code/name)로 참조하므로 PlaceSeeder보다 먼저 시딩돼야 한다.
-- default_image_url은 사진 업로드(S3) 단계 전이라 아직 비워둔다.

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
