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
