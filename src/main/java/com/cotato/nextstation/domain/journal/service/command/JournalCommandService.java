package com.cotato.nextstation.domain.journal.service.command;


import com.cotato.nextstation.domain.journal.dto.request.JournalCreateRequest;
import com.cotato.nextstation.domain.journal.dto.request.JournalUpdateRequest;
import com.cotato.nextstation.domain.place.dto.request.PlaceReviewCreateRequest;
import com.cotato.nextstation.domain.journal.entity.Journal;
import com.cotato.nextstation.domain.journal.entity.JournalImage;
import com.cotato.nextstation.domain.journal.enums.TravelDuration;
import com.cotato.nextstation.domain.journal.exception.JournalErrorCode;
import com.cotato.nextstation.domain.journal.repository.JournalImageRepository;
import com.cotato.nextstation.domain.journal.repository.JournalRepository;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.domain.place.dto.request.PlaceReviewUpdateRequest;
import com.cotato.nextstation.domain.place.service.command.PlaceReviewCommandService;
import com.cotato.nextstation.domain.stamp.service.query.MemberStampQueryService;

import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class JournalCommandService {

    private final JournalRepository journalRepository;
    private final MemberRepository memberRepository;
    private final JournalImageRepository journalImageRepository;

    private final MemberStampQueryService memberStampQueryService;
    private final PlaceReviewCommandService placeReviewCommandService;

    // 여행일지 작성
    public Long createJournal(Long memberId, JournalCreateRequest request) {
        // memberStamp 소유권 검증
        memberStampQueryService.getCourseId(memberId, request.memberStampId());

        Member member = memberRepository.getReferenceById(memberId);


        // 여행일지 생성
        Journal journal = Journal.builder()
                .member(member)
                .memberStampId(request.memberStampId())
                .title(request.title())
                .overallReview(request.overallReview())
                .traveledAt(request.traveledAt())
                .travelDuration(request.travelDuration())
                .isPublic(request.isPublic())
                .build();
        journalRepository.save(journal);

        // 대표 사진 저장 (순서대로, 첫 번째가 대표)
        // 프론트가 보낸 imageUrls를 journal에 연결하는 부분
        // S3에는 이미 올라가 있고, 그 URL만 DB에 연결
        if (request.journalImageUrls() != null) {
            request.journalImageUrls().forEach(imageUrl ->
                    journalImageRepository.save(
                            JournalImage.builder()
                                    .journal(journal)
                                    .imageUrl(imageUrl)
                                    .build()
                    )
            );
        }
            // 장소 리뷰 저장
        List<PlaceReviewCreateRequest> reviewRequests =
                request.placeReviews() == null ? List.of() :
                        request.placeReviews().stream()
                                .map(pr -> new PlaceReviewCreateRequest(
                                        pr.placeId(),
                                        pr.review(),
                                        pr.imageUrl()
                                ))
                                .toList();


        placeReviewCommandService.createPlaceReviews(journal, reviewRequests);

        log.info("여행일지 작성 완료: memberId={}, journalId={}", memberId, journal.getId());

            return journal.getId();
        }

    // 여행일지 수정
    public void updateJournal(Long memberId, Long journalId, JournalUpdateRequest request) {
        Journal journal = findOwnJournal(memberId, journalId);

        // 기본 필드 수정 (null이면 기존값 유지)
        journal.update(
                Optional.ofNullable(request.title()).orElse(journal.getTitle()),
                Optional.ofNullable(request.overallReview()).orElse(journal.getOverallReview()),
                Optional.ofNullable(request.traveledAt()).orElse(journal.getTraveledAt()),
                Optional.ofNullable(request.travelDuration()).orElse(journal.getTravelDuration()),
                Optional.ofNullable(request.isPublic()).orElse(journal.isPublic())
        );


        // 여행일지 사진 수정
        if (request.journalPhotos() != null) {
            request.journalPhotos().forEach(photo -> {
                switch (photo.imageAction()) {
                    case KEEP -> {
                        // 유지
                    }
                    case DELETE -> {
                        // photoId로 DB에서 삭제 (S3는 배치 잡으로 정리)
                        journalImageRepository.deleteById(photo.photoId());
                    }
                    case UPDATE -> {
                        // 새 이미지 추가 (photoId 없음)
                        journalImageRepository.save(
                                JournalImage.builder()
                                        .journal(journal)
                                        .imageUrl(photo.imageUrl())
                                        .build()
                        );
                    }
                }
            });
        }

        // 장소 리뷰 수정 (보내지 않은 장소는 KEEP으로 간주)
        if (request.placeReviews() != null) {

            placeReviewCommandService.updatePlaceReviews(journal, request.placeReviews());
        }

        log.info("여행일지 수정 완료: memberId={}, journalId={}", memberId, journalId);
    }

    // 여행일지 삭제 -> 리뷰는 삭제하되, 장소 데이터는 DB에 남겨두기
    public void deleteJournal(Long memberId, Long journalId) {
        Journal journal = findOwnJournal(memberId, journalId);


        // 여행일지 대표 사진만 DB에서 삭제 (PlaceReview/PlaceReviewImage는 유지)
        journalImageRepository.deleteByJournalId(journalId);

        // 여행일지 soft delete
        journal.delete();
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