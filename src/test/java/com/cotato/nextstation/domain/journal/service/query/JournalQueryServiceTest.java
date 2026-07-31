package com.cotato.nextstation.domain.journal.service.query;

import com.cotato.nextstation.domain.course.dto.response.CourseInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceInfoResponse;
import com.cotato.nextstation.domain.course.service.command.CourseCommandService;
import com.cotato.nextstation.domain.course.service.query.CourseQueryService;
import com.cotato.nextstation.domain.journal.converter.JournalConverter;
import com.cotato.nextstation.domain.journal.dto.response.JournalDetailResponse;
import com.cotato.nextstation.domain.journal.entity.Journal;
import com.cotato.nextstation.domain.journal.enums.TravelDuration;
import com.cotato.nextstation.domain.journal.repository.JournalImageRepository;
import com.cotato.nextstation.domain.journal.repository.JournalRepository;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.repository.PlaceReviewImageRepository;
import com.cotato.nextstation.domain.place.repository.PlaceReviewRepository;
import com.cotato.nextstation.domain.place.service.query.PlaceInfoQueryService;
import com.cotato.nextstation.domain.stamp.service.query.MemberStampQueryService;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.service.query.StationQueryService;
import com.cotato.nextstation.global.exception.CustomException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * JournalQueryService 테스트. 코스 상세(courseId/isMine/isLiked) 연동 및 조회수 반영 검증에 집중한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JournalQueryServiceTest {

    @Mock
    private MemberStampQueryService memberStampQueryService;
    @Mock
    private CourseQueryService courseQueryService;
    @Mock
    private CourseCommandService courseCommandService;
    @Mock
    private PlaceInfoQueryService placeInfoQueryService;
    @Mock
    private StationQueryService stationQueryService;
    @Mock
    private JournalRepository journalRepository;
    @Mock
    private JournalImageRepository journalImageRepository;
    @Mock
    private PlaceReviewRepository placeReviewRepository;
    @Mock
    private PlaceReviewImageRepository placeReviewImageRepository;

    private JournalQueryService journalQueryService;

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final Long JOURNAL_ID = 10L;
    private static final Long MEMBER_STAMP_ID = 100L;
    private static final Long COURSE_ID = 999L;
    private static final Long STATION_ID = 6L;
    private static final Long PLACE_ID = 12L;

    private Journal journal;

    @BeforeEach
    void setUp() {
        // JournalConverter는 의존성 없는 순수 변환기라 목이 아닌 실제 인스턴스를 쓴다.
        journalQueryService = new JournalQueryService(
                memberStampQueryService, courseQueryService, courseCommandService,
                placeInfoQueryService, stationQueryService,
                journalRepository, journalImageRepository,
                placeReviewRepository, placeReviewImageRepository,
                new JournalConverter());

        Member owner = mock(Member.class);
        given(owner.getId()).willReturn(OWNER_ID);
        given(owner.getNickname()).willReturn("현주");

        journal = Journal.builder()
                .member(owner)
                .memberStampId(MEMBER_STAMP_ID)
                .title("보문 골목 산책")
                .traveledAt(LocalDate.of(2026, 7, 8))
                .travelDuration(TravelDuration.HALF_DAY)
                .isPublic(true)
                .build();
        ReflectionTestUtils.setField(journal, "id", JOURNAL_ID);

        given(journalRepository.findById(JOURNAL_ID)).willReturn(Optional.of(journal));
        given(memberStampQueryService.getCourseId(OWNER_ID, MEMBER_STAMP_ID)).willReturn(COURSE_ID);
        given(courseQueryService.getCourseInfo(COURSE_ID)).willReturn(
                new CourseInfoResponse(COURSE_ID, "보문에 살어리랏다", OWNER_ID, STATION_ID, JOURNAL_ID, 10, 3, null));
        given(stationQueryService.getStationName(STATION_ID)).willReturn("보문역");
        given(stationQueryService.getLine(STATION_ID))
                .willReturn(new LineSummaryResponse(1L, "우이신설선", null));
        given(courseQueryService.getCoursePlaces(COURSE_ID))
                .willReturn(List.of(new CoursePlaceInfoResponse(PLACE_ID, 1)));
        given(placeInfoQueryService.getPlaceInfos(anyList())).willReturn(List.of(
                new PlaceInfoResponse(PLACE_ID, "보문숲길도서관", "설명", "CULTURE", "문화공간", null, 127.123, 37.456)));
        given(placeInfoQueryService.getTopTagNames(anyList())).willReturn(List.of());
        given(journalImageRepository.findByJournalIdOrderByIdAsc(JOURNAL_ID)).willReturn(List.of());
        given(placeReviewRepository.findByJournalId(JOURNAL_ID)).willReturn(List.of());
        given(placeReviewImageRepository.findByPlaceReviewIdIn(anyList())).willReturn(List.of());
    }

    @Nested
    @DisplayName("getJournalDetail")
    class GetJournalDetail {

        @Test
        @DisplayName("본인이 조회하면 courseId/isMine=true가 채워지고 조회수는 증가시키지 않는다")
        void ownerViews_fillsCourseIdAndIsMine() {
            // given
            given(courseQueryService.isLikedByMember(COURSE_ID, OWNER_ID)).willReturn(false);

            // when
            JournalDetailResponse response = journalQueryService.getJournalDetail(OWNER_ID, JOURNAL_ID);

            // then
            assertThat(response.courseId()).isEqualTo(COURSE_ID);
            assertThat(response.isMine()).isTrue();
            assertThat(response.isLiked()).isFalse();
            verify(courseCommandService).increaseViewCount(COURSE_ID, OWNER_ID);
        }

        @Test
        @DisplayName("타인이 좋아요를 눌러둔 코스를 조회하면 isMine=false, isLiked=true가 채워진다")
        void otherMemberViews_fillsIsLiked() {
            // given
            given(courseQueryService.isLikedByMember(COURSE_ID, OTHER_MEMBER_ID)).willReturn(true);

            // when
            JournalDetailResponse response = journalQueryService.getJournalDetail(OTHER_MEMBER_ID, JOURNAL_ID);

            // then
            assertThat(response.isMine()).isFalse();
            assertThat(response.isLiked()).isTrue();
            assertThat(response.visitedPlaces()).hasSize(1);
            assertThat(response.visitedPlaces().get(0).xCoordinate()).isEqualTo(127.123);
            assertThat(response.visitedPlaces().get(0).yCoordinate()).isEqualTo(37.456);
            verify(courseCommandService).increaseViewCount(COURSE_ID, OTHER_MEMBER_ID);
        }

        @Test
        @DisplayName("비로그인 조회도 조회수 반영 호출은 그대로 나간다 (본인 제외 판단은 CourseCommandService 몫)")
        void anonymousViews_stillCallsIncreaseViewCount() {
            // given
            given(courseQueryService.isLikedByMember(COURSE_ID, null)).willReturn(false);

            // when
            journalQueryService.getJournalDetail(null, JOURNAL_ID);

            // then
            verify(courseCommandService).increaseViewCount(COURSE_ID, null);
        }

        @Test
        @DisplayName("타인이 비공개 일지를 조회하면 JOURNAL_FORBIDDEN 예외를 던지고 조회수 증가 호출은 나가지 않는다")
        void otherMemberViewsPrivateJournal_throwsForbiddenAndNeverIncreasesViewCount() {
            // given: setUp의 journal은 공개 상태라, 이 테스트만 비공개로 재정의한다
            Journal privateJournal = Journal.builder()
                    .member(journal.getMember())
                    .memberStampId(MEMBER_STAMP_ID)
                    .title("보문 골목 산책")
                    .traveledAt(LocalDate.of(2026, 7, 8))
                    .travelDuration(TravelDuration.HALF_DAY)
                    .isPublic(false)
                    .build();
            ReflectionTestUtils.setField(privateJournal, "id", JOURNAL_ID);
            given(journalRepository.findById(JOURNAL_ID)).willReturn(Optional.of(privateJournal));

            // when & then: 권한 검증에서 막혀야 하고, 그 뒤에 있는 조회수 증가 호출까지 가면 안 된다
            assertThatThrownBy(() -> journalQueryService.getJournalDetail(OTHER_MEMBER_ID, JOURNAL_ID))
                    .isInstanceOf(CustomException.class);

            verify(courseCommandService, never()).increaseViewCount(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("getTravelDuration")
    class GetTravelDuration {

        @Test
        @DisplayName("journalId가 null이면 findById를 호출하지 않고 null을 반환한다")
        void nullJournalId_returnsNullWithoutQuery() {
            // when & then
            assertThatCode(() -> {
                TravelDuration result = journalQueryService.getTravelDuration(null);
                assertThat(result).isNull();
            }).doesNotThrowAnyException();

            verify(journalRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("존재하는 journalId면 해당 일지의 travelDuration을 반환한다")
        void existingJournalId_returnsTravelDuration() {
            // when
            TravelDuration result = journalQueryService.getTravelDuration(JOURNAL_ID);

            // then
            assertThat(result).isEqualTo(TravelDuration.HALF_DAY);
        }
    }
}
