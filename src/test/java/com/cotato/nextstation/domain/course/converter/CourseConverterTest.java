package com.cotato.nextstation.domain.course.converter;

import com.cotato.nextstation.domain.course.dto.response.CourseCardResponse;
import com.cotato.nextstation.domain.course.dto.response.LikedCourseListResponse;
import com.cotato.nextstation.domain.course.dto.response.MemberCourseCardResponse;
import com.cotato.nextstation.domain.course.repository.CourseLikeRepository.LikedCourseView;
import com.cotato.nextstation.domain.course.dto.response.MyCourseDetailResponse;
import com.cotato.nextstation.domain.course.dto.response.MyCoursePlaceResponse;
import com.cotato.nextstation.domain.course.dto.response.PlaceCourseResponse;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.course.repository.CourseRepository.MemberCourseCardView;
import com.cotato.nextstation.domain.course.repository.CourseRepository.MyCourseDetailView;
import com.cotato.nextstation.domain.journal.enums.TravelDuration;
import com.cotato.nextstation.domain.course.repository.CourseRepository.PlaceCourseView;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.LineCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class CourseConverterTest {

    private final CourseConverter courseConverter = new CourseConverter();

    private PlaceCourseView view() {
        PlaceCourseView view = mock(PlaceCourseView.class);
        given(view.getCourseId()).willReturn(10L);
        given(view.getName()).willReturn("주연의 보문역 여행");
        given(view.getStationId()).willReturn(123L);
        given(view.getStationName()).willReturn("보문역");
        given(view.getLineId()).willReturn(6L);
        given(view.getLineName()).willReturn("6호선");
        given(view.getLineCode()).willReturn(LineCode.LINE_6);
        return view;
    }

    private MemberCourseCardView memberCourseCardView() {
        MemberCourseCardView view = mock(MemberCourseCardView.class);
        given(view.getCourseId()).willReturn(7L);
        given(view.getJournalId()).willReturn(20L);
        given(view.getName()).willReturn("보문역 환승여행 코스");
        given(view.getStationId()).willReturn(6L);
        given(view.getStationName()).willReturn("보문역");
        given(view.getLineId()).willReturn(6L);
        given(view.getLineName()).willReturn("6호선");
        given(view.getLineCode()).willReturn(LineCode.LINE_6);
        given(view.getLikeCount()).willReturn(12);
        return view;
    }

    private MyCourseDetailView detailView(Long lineId, String lineName, LineCode lineCode) {
        MyCourseDetailView view = mock(MyCourseDetailView.class);
        given(view.getCourseId()).willReturn(1L);
        given(view.getName()).willReturn("민성이랑 떠나는 느좋투어");
        given(view.getStationId()).willReturn(6L);
        given(view.getStationName()).willReturn("신림역");
        given(view.getLineId()).willReturn(lineId);
        given(view.getLineName()).willReturn(lineName);
        given(view.getLineCode()).willReturn(lineCode);
        return view;
    }

    @Test
    @DisplayName("코스 확인 응답에 역의 대표 호선을 담는다")
    void toMyCourseDetailResponse_withLine() {
        // when: 화면 상단 배지가 호선에 따라 달라진다
        MyCourseDetailResponse response = courseConverter.toMyCourseDetailResponse(
                detailView(2L, "2호선", LineCode.LINE_2), List.of());

        // then
        assertThat(response.stationName()).isEqualTo("신림역");
        assertThat(response.line().id()).isEqualTo(2L);
        assertThat(response.line().name()).isEqualTo("2호선");
        assertThat(response.line().code()).isEqualTo(LineCode.LINE_2);
    }

    @Test
    @DisplayName("대표 호선이 없는 역이면 line을 null로 내린다")
    void toMyCourseDetailResponse_withoutLine() {
        // given: 뽑기 대상이 아닌 역은 대표 호선이 비어 있을 수 있다
        MyCourseDetailResponse response = courseConverter.toMyCourseDetailResponse(
                detailView(null, null, null), List.of());

        // then: 셋 다 null이므로 노선 객체 자체를 내리지 않는다
        assertThat(response.line()).isNull();
    }

    @Test
    @DisplayName("코스 확인 장소 응답에 조회한 장소 정보를 순서와 함께 그대로 담는다")
    void toMyCoursePlaceResponse() {
        // given
        PlaceInfoResponse place = new PlaceInfoResponse(
                11L, "보문숲길도서관", "혼자 조용히 머물기 좋은 동네 도서관",
                "CULTURE", "문화공간", "https://img/1.jpg", 127.0345, 37.5804);

        // when
        MyCoursePlaceResponse response = courseConverter.toMyCoursePlaceResponse(place, 2);

        // then: 필드가 하나라도 누락되면 화면에서 핀/이미지/순서가 어긋난다
        assertThat(response).isEqualTo(new MyCoursePlaceResponse(
                11L, "보문숲길도서관", "혼자 조용히 머물기 좋은 동네 도서관",
                "CULTURE", "문화공간", "https://img/1.jpg", 127.0345, 37.5804, 2));
    }

    @Test
    @DisplayName("장소 이미지가 없어도 카테고리는 담아 내린다")
    void toMyCoursePlaceResponse_withoutImage() {
        // given: 카테고리 기본 이미지가 아직 없어 imageUrl이 비는 장소
        PlaceInfoResponse place = new PlaceInfoResponse(
                12L, "보문사", "천년 고찰", "CULTURE", "문화공간", null, 127.0350, 37.5810);

        // when
        MyCoursePlaceResponse response = courseConverter.toMyCoursePlaceResponse(place, 1);

        // then: 이 경우 프론트가 카테고리로 대체 이미지를 고른다
        assertThat(response.imageUrl()).isNull();
        assertThat(response.categoryCode()).isEqualTo("CULTURE");
        assertThat(response.categoryName()).isEqualTo("문화공간");
    }

    @ParameterizedTest(name = "장소 {0}곳이면 {1}")
    @DisplayName("소요시간 구간은 장소 수로 추정한다")
    @CsvSource({
            "3, SHORT",
            "4, SHORT",
            "5, HALF_DAY",
            "7, HALF_DAY",
            "8, FULL_DAY",
            "10, FULL_DAY"
    })
    void estimateDuration(int placeCount, String expected) {
        // when: 여행기록의 "코스 시간" 선택지와 같은 구간을 쓴다
        PlaceCourseResponse response = courseConverter.toPlaceCourseResponse(view(), placeCount, List.of(), null, null);

        // then
        assertThat(response.travelDuration()).isEqualTo(expected);
    }

    @Test
    @DisplayName("장소가 코스 최소 개수보다 적어도 가장 짧은 구간으로 내려간다")
    void estimateDuration_lowerBound() {
        // given: 코스는 장소 3개 이상이지만, 데이터가 어긋나도 이상한 값이 나가지 않아야 한다
        assertThat(courseConverter.toPlaceCourseResponse(view(), 1, List.of(), null, null).travelDuration())
                .isEqualTo("SHORT");
        assertThat(courseConverter.toPlaceCourseResponse(view(), 0, List.of(), null, null).travelDuration())
                .isEqualTo("SHORT");
    }

    @Test
    @DisplayName("여행일지에 소요시간이 있으면 장소 수 추정 대신 그 값을 쓴다")
    void travelDurationFromJournal() {
        // given: 장소 8곳이면 추정값은 FULL_DAY지만, 실제로 다녀온 사람이 남긴 값이 우선이다
        PlaceCourseResponse response = courseConverter.toPlaceCourseResponse(
                view(), 8, List.of(), null, TravelDuration.SHORT);

        // then
        assertThat(response.travelDuration()).isEqualTo("SHORT");
    }

    @Test
    @DisplayName("코스 카드에 역/호선 정보를 그대로 담는다")
    void toPlaceCourseResponse() {
        // when
        PlaceCourseResponse response = courseConverter.toPlaceCourseResponse(
                view(), 4, List.of("자연과함께", "사진찍기좋은"), "cover.jpg", null);

        // then
        assertThat(response.courseId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("주연의 보문역 여행");
        assertThat(response.stationId()).isEqualTo(123L);
        assertThat(response.stationName()).isEqualTo("보문역");
        assertThat(response.line().id()).isEqualTo(6L);
        assertThat(response.line().name()).isEqualTo("6호선");
        assertThat(response.line().code()).isEqualTo(LineCode.LINE_6);
        assertThat(response.placeCount()).isEqualTo(4);
        assertThat(response.tags()).containsExactly("자연과함께", "사진찍기좋은");
        assertThat(response.imageUrl()).isEqualTo("cover.jpg");
    }

    @Test
    @DisplayName("좋아요한 코스 카드에 여행일지 ID를 담는다")
    void toLikedListResponse() {
        // given
        LikedCourseView view = mock(LikedCourseView.class);
        given(view.getCourseId()).willReturn(7L);
        given(view.getJournalId()).willReturn(20L);
        given(view.getName()).willReturn("보문역 환승여행 코스");
        given(view.getStationId()).willReturn(6L);
        given(view.getStationName()).willReturn("보문역");
        given(view.getLineId()).willReturn(6L);
        given(view.getLineName()).willReturn("6호선");
        given(view.getLineCode()).willReturn(LineCode.LINE_6);

        // when
        LikedCourseListResponse response = courseConverter.toLikedListResponse(List.of(view), "cursor", true);

        // then: journalId가 빠지면 카드를 눌러도 여행일지 상세를 열 수 없다
        assertThat(response.courses()).containsExactly(new CourseCardResponse(
                7L, 20L, "보문역 환승여행 코스", 6L, "보문역",
                new LineSummaryResponse(6L, "6호선", LineCode.LINE_6)));
    }

    @Test
    @DisplayName("다른 회원 공개 코스 카드에 journalId·imageUrl·likeCount를 담는다")
    void toMemberCourseCardResponse() {
        // when
        MemberCourseCardResponse response = courseConverter.toMemberCourseCardResponse(
                memberCourseCardView(), "journal-cover.jpg");

        // then: 필드가 하나라도 어긋나면 카드 이동(journalId)이나 디자인(imageUrl/likeCount)이 깨진다
        assertThat(response).isEqualTo(new MemberCourseCardResponse(
                7L, 20L, "보문역 환승여행 코스", 6L, "보문역",
                new LineSummaryResponse(6L, "6호선", LineCode.LINE_6),
                "journal-cover.jpg", 12));
    }

    @Test
    @DisplayName("다른 회원 공개 코스 카드는 대표 호선이 없으면 line을 null로 내린다")
    void toMemberCourseCardResponse_withoutLine() {
        // given
        MemberCourseCardView view = memberCourseCardView();
        given(view.getLineId()).willReturn(null);
        given(view.getLineName()).willReturn(null);
        given(view.getLineCode()).willReturn(null);

        // when
        MemberCourseCardResponse response = courseConverter.toMemberCourseCardResponse(view, null);

        // then
        assertThat(response.line()).isNull();
        assertThat(response.imageUrl()).isNull();
    }
}
