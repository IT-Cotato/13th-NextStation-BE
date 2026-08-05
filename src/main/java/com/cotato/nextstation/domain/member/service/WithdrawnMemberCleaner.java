package com.cotato.nextstation.domain.member.service;

import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 유예 기간이 끝난 탈퇴 회원을 회원 행까지 통째로 지운다(hard delete).
 * <p>
 * 회원이 남긴 콘텐츠(일지·코스·리뷰·사진)와 다른 회원이 그 콘텐츠에 남긴 좋아요까지 함께 사라진다.
 * FK 제약 때문에 자식 → 부모 순서로 지워야 하므로 SQL 순서를 바꾸지 말 것.
 * <p>
 * 하루치 대상이 많아야 수십 명이고 실패하면 롤백 후 다음 날 다시 집으면 되므로
 * Spring Batch(청크·재시작·메타데이터 테이블) 없이 @Scheduled 하나로 끝낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawnMemberCleaner {

    private static final String JOURNAL_IDS = "SELECT id FROM journal WHERE member_id IN (:ids)";
    private static final String COURSE_IDS = "SELECT id FROM course WHERE member_id IN (:ids)";
    private static final String REVIEW_IDS = "SELECT id FROM place_review WHERE journal_id IN (" + JOURNAL_IDS + ")";

    // 자식 → 부모 순서. 각 문장의 :ids는 파기 대상 회원 ID 목록.
    // TODO: 테이블 목록이 수동 관리 - member_id를 갖는 엔티티가 늘면 여기에도 추가해야 한다.
    // 빠뜨리면 마지막 DELETE FROM member가 FK 위반으로 터지고 전체 롤백되므로 조용히 깨지진 않는다.
    // IN (:ids)도 청크 분할이 없다. 하루치 만료 회원이 수천 명 되는 규모가 오면 그때 나눌 것.
    private static final List<String> DELETE_STATEMENTS = List.of(
            "DELETE FROM place_image WHERE place_review_id IN (" + REVIEW_IDS + ")",
            "DELETE FROM place_review_image WHERE place_review_id IN (" + REVIEW_IDS + ")",
            // 내가 누른 좋아요 + 내 리뷰에 남의 좋아요, 둘 다 지운다
            "DELETE FROM place_review_like WHERE member_id IN (:ids) OR place_review_id IN (" + REVIEW_IDS + ")",
            "DELETE FROM place_review WHERE journal_id IN (" + JOURNAL_IDS + ")",
            "DELETE FROM journal_image WHERE journal_id IN (" + JOURNAL_IDS + ")",
            "DELETE FROM course_like WHERE member_id IN (:ids) OR course_id IN (" + COURSE_IDS + ")",
            "DELETE FROM member_place_stamps WHERE member_id IN (:ids) OR course_id IN (" + COURSE_IDS + ")",
            "DELETE FROM course_places WHERE course_id IN (" + COURSE_IDS + ")",
            "DELETE FROM course WHERE member_id IN (:ids)",
            "DELETE FROM journal WHERE member_id IN (:ids)",
            "DELETE FROM member_terms_agreement WHERE member_id IN (:ids)",
            "DELETE FROM member_social_account WHERE member_id IN (:ids)",
            "DELETE FROM email_verification WHERE member_id IN (:ids)",
            "DELETE FROM recommendation_log WHERE member_id IN (:ids)",
            "DELETE FROM member WHERE id IN (:ids)"
    );

    private final MemberRepository memberRepository;
    private final EntityManager entityManager;

    // 매일 새벽 4시 30분. 같은 시간대의 EmailVerificationCleaner(4시)와 겹치지 않게 띄운다.
    // 하루 한 번이라 실제 삭제는 유예 만료 시점에서 최대 하루 늦게 일어난다.
    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public void purgeExpiredWithdrawals() {
        LocalDateTime threshold = LocalDateTime.now().minus(Member.WITHDRAWAL_GRACE_PERIOD);
        List<Long> targetIds = memberRepository.findIdsByStatusAndDeletedAtBefore(MemberStatus.WITHDRAWN, threshold);

        if (targetIds.isEmpty()) {
            log.info("파기 대상 탈퇴 회원 없음: threshold={}", threshold);
            return;
        }

        for (String sql : DELETE_STATEMENTS) {
            int deleted = entityManager.createNativeQuery(sql)
                    .setParameter("ids", targetIds)
                    .executeUpdate();
            log.info("탈퇴 회원 데이터 삭제: table={}, rows={}", sql.split(" ")[2], deleted);
        }

        log.info("탈퇴 회원 파기 완료: memberIds={}, threshold={}", targetIds, threshold);
    }
}