package com.cotato.nextstation.domain.journal.service.command;

import com.cotato.nextstation.domain.course.dto.response.CourseInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceInfoResponse;
import com.cotato.nextstation.domain.course.service.query.CourseQueryService;
import com.cotato.nextstation.domain.journal.dto.request.JournalCreateRequest;
import com.cotato.nextstation.domain.journal.dto.response.UncompletedJournalListResponse;
import com.cotato.nextstation.domain.journal.entity.Journal;
import com.cotato.nextstation.domain.journal.entity.JournalImage;
import com.cotato.nextstation.domain.journal.enums.TravelDuration;
import com.cotato.nextstation.domain.journal.exception.JournalErrorCode;
import com.cotato.nextstation.domain.journal.repository.JournalImageRepository;
import com.cotato.nextstation.domain.journal.repository.JournalRepository;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.entity.Place;
import com.cotato.nextstation.domain.place.entity.PlaceReview;
import com.cotato.nextstation.domain.place.entity.PlaceReviewImage;
import com.cotato.nextstation.domain.place.exception.PlaceErrorCode;
import com.cotato.nextstation.domain.place.repository.PlaceRepository;
import com.cotato.nextstation.domain.place.repository.PlaceReviewImageRepository;
import com.cotato.nextstation.domain.place.repository.PlaceReviewRepository;
import com.cotato.nextstation.domain.place.service.query.PlaceInfoQueryService;
import com.cotato.nextstation.domain.stamp.entity.MemberStamp;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository;
import com.cotato.nextstation.domain.stamp.service.query.MemberStampQueryService;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.service.query.StationQueryService;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class JournalCommandService {

    private static final int TAGS_PER_CARD = 2;

    private final JournalRepository journalRepository;
    private final MemberRepository memberRepository;
    private final MemberStampRepository memberStampRepository;
    private final JournalImageRepository journalImageRepository;
    private final PlaceRepository placeRepository;
    private final PlaceReviewRepository placeReviewRepository;
    private final PlaceReviewImageRepository placeReviewImageRepository;

    private final StationQueryService stationQueryService;
    private final MemberStampQueryService memberStampQueryService;
    private final CourseQueryService courseQueryService;
    private final PlaceInfoQueryService placeInfoQueryService;

    // 여행일지 작성
    public Long createJournal(Long memberId, JournalCreateRequest request) {
        // memberStamp 소유권 검증
        Long courseId = memberStampQueryService.getCourseId(memberId, request.memberStampId());
        Member member = memberRepository.getReferenceById(memberId);
        MemberStamp memberStamp = memberStampRepository.getReferenceById(request.memberStampId());


        // 여행일지 생성
        Journal journal = Journal.builder()
                .member(member)
                .memberStamp(memberStamp)
                .title(request.title())
                .overallReview(request.overallReview())
                .traveledAt(request.traveledAt())
                .travelDuration(request.travelDuration())
                .isPublic(request.isPublic())
                .build();
        Journal saved = journalRepository.save(journal);

        // 대표 사진 저장
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
            if (request.placeReviews() != null) {
                request.placeReviews().forEach(pr -> {
                    Place place = placeRepository.findById(pr.placeId())
                            .orElseThrow(() -> new CustomException(PlaceErrorCode.PLACE_NOT_FOUND));

                    PlaceReview placeReview = PlaceReview.builder()
                            .place(place)
                            .journal(journal)
                            .review(pr.review())
                            .build();
                    placeReviewRepository.save(placeReview);

                    // 리뷰 이미지 (1개만)
                    if (pr.imageUrl() != null) {
                        placeReviewImageRepository.save(
                                PlaceReviewImage.builder()
                                        .placeReview(placeReview)
                                        .imageUrl(pr.imageUrl())
                                        .build()
                        );
                    }
                });
            }

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