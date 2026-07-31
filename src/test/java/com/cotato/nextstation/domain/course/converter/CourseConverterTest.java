package com.cotato.nextstation.domain.course.converter;

import com.cotato.nextstation.domain.course.dto.response.MyCourseDetailResponse;
import com.cotato.nextstation.domain.course.dto.response.PlaceCourseResponse;
import com.cotato.nextstation.domain.course.repository.CourseRepository.MyCourseDetailView;
import com.cotato.nextstation.domain.course.repository.CourseRepository.PlaceCourseView;
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
        PlaceCourseResponse response = courseConverter.toPlaceCourseResponse(view(), placeCount, List.of(), null);

        // then
        assertThat(response.travelDuration()).isEqualTo(expected);
    }

    @Test
    @DisplayName("장소가 코스 최소 개수보다 적어도 가장 짧은 구간으로 내려간다")
    void estimateDuration_lowerBound() {
        // given: 코스는 장소 3개 이상이지만, 데이터가 어긋나도 이상한 값이 나가지 않아야 한다
        assertThat(courseConverter.toPlaceCourseResponse(view(), 1, List.of(), null).travelDuration())
                .isEqualTo("SHORT");
        assertThat(courseConverter.toPlaceCourseResponse(view(), 0, List.of(), null).travelDuration())
                .isEqualTo("SHORT");
    }

    @Test
    @DisplayName("코스 카드에 역/호선 정보를 그대로 담는다")
    void toPlaceCourseResponse() {
        // when
        PlaceCourseResponse response = courseConverter.toPlaceCourseResponse(
                view(), 4, List.of("자연과함께", "사진찍기좋은"), "cover.jpg");

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
}
