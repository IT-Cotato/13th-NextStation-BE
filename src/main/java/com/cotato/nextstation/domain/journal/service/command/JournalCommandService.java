package com.cotato.nextstation.domain.journal.service.command;

import com.cotato.nextstation.domain.journal.entity.Journal;
import com.cotato.nextstation.domain.journal.enums.TravelDuration;
import com.cotato.nextstation.domain.journal.exception.JournalErrorCode;
import com.cotato.nextstation.domain.journal.repository.JournalRepository;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class JournalCommandService {

    private final JournalRepository journalRepository;
    private final MemberRepository memberRepository;
    private final MemberStampRepository memberStampRepository;

    // 여행일지 작성
    public Long createJournal(Long memberId, Long memberStampId, String title,
                              String overallReview, LocalDate traveledAt,
                              TravelDuration travelDuration, boolean isPublic) {
        // memberStamp 소유권 검증
        if (!memberStampRepository.existsByMemberIdAndId(memberId, memberStampId)) {
            throw new CustomException(JournalErrorCode.MEMBER_STAMP_NOT_FOUND);
        }

        Member member = memberRepository.getReferenceById(memberId);

        Journal journal = Journal.builder()
                .member(member)
                .memberStampId(memberStampId)
                .title(title)
                .overallReview(overallReview)
                .traveledAt(traveledAt)
                .travelDuration(travelDuration)
                .isPublic(isPublic)
                .build();

        Journal saved = journalRepository.save(journal);
        log.info("여행일지 작성 완료: memberId={}, journalId={}", memberId, saved.getId());
        return saved.getId();
    }

    // 여행일지 수정
    public void updateJournal(Long memberId, Long journalId, String title,
                              String overallReview, LocalDate traveledAt,
                              TravelDuration travelDuration, boolean isPublic) {
        Journal journal = findOwnJournal(memberId, journalId);
        journal.update(title, overallReview, traveledAt, travelDuration, isPublic);
        log.info("여행일지 수정 완료: memberId={}, journalId={}", memberId, journalId);
    }

    // 여행일지 삭제
    public void deleteJournal(Long memberId, Long journalId) {
        Journal journal = findOwnJournal(memberId, journalId);
        journal.delete();
        log.info("여행일지 삭제 완료: memberId={}, journalId={}", memberId, journalId);
    }

    private Journal findOwnJournal(Long memberId, Long journalId) {
        Journal journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new CustomException(JournalErrorCode.JOURNAL_NOT_FOUND));
        if (!journal.getMember().getId().equals(memberId)) {
            throw new CustomException(JournalErrorCode.JOURNAL_FORBIDDEN);
        }
        return journal;
    }
}