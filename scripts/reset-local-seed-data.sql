-- 로컬 개발 DB의 station/line/place 계열 시딩 데이터를 초기화한다.
-- StationDataSeeder / PlaceSeeder는 테이블에 데이터가 있으면 스킵하므로,
-- 시딩 로직(csv, LineCode 등)을 바꾼 뒤 재검증하려면 먼저 이 스크립트로 비우고 앱을 재기동해야 한다.
--
-- 실행 예:
--   docker exec -i 13th-nextstation-be-mysql-1 mysql -uroot -proot nextstation-local < scripts/reset-local-seed-data.sql
--
-- 대상: 로컬 개발 DB 전용. 운영 DB에는 절대 실행하지 말 것.

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE place_review_image;
TRUNCATE TABLE place_review_like;
TRUNCATE TABLE place_image;
TRUNCATE TABLE place_review;
TRUNCATE TABLE place_tag_mapping;
TRUNCATE TABLE place;
TRUNCATE TABLE station_line;
TRUNCATE TABLE station;
TRUNCATE TABLE line;

-- 선택: station/place를 FK 없이 Long 식별자로만 참조하는 사용자 데이터도 같이 비우고 싶다면 주석 해제
-- TRUNCATE TABLE member_departure_station;
-- TRUNCATE TABLE recommendation_log;
-- TRUNCATE TABLE course_places;

SET FOREIGN_KEY_CHECKS = 1;