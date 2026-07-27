package com.cotato.nextstation.domain.stamp.service.command;

import com.cotato.nextstation.domain.course.dto.response.CourseInfoResponse;
import com.cotato.nextstation.domain.course.service.query.CourseQueryService;
import com.cotato.nextstation.domain.stamp.dto.response.CourseCompleteResponse;
import com.cotato.nextstation.domain.stamp.entity.MemberStamp;
import com.cotato.nextstation.domain.stamp.exception.StampErrorCode;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StampCommandServiceTest {

    @InjectMocks
    private StampCommandService stampCommandService;

    @Mock
    private CourseQueryService courseQueryService;
    @Mock
    private MemberStampRepository memberStampRepository;
    @Mock
    private StationRepository stationRepository;

    @Test
    @DisplayName("코스 완료 처리 시 MemberStamp가 생성되고 응답을 반환한다")
    void completeCourse_success() {
        // given
        Long memberId = 1L;
        Long courseId = 10L;
        Long stationId = 12L;

        CourseInfoResponse courseInfo = new CourseInfoResponse(
                courseId, "보문역 코스", memberId, stationId, null, 0, 0, LocalDateTime.now()
        );

        MemberStamp memberStamp = mock(MemberStamp.class);
        given(memberStamp.getId()).willReturn(501L);
        given(memberStamp.getCreatedAt()).willReturn(LocalDateTime.of(2026, 7, 7, 18, 30));

        Station station = mock(Station.class);
        given(station.getId()).willReturn(stationId);
        given(station.getStationName()).willReturn("보문역");

        given(courseQueryService.getCourseInfo(courseId)).willReturn(courseInfo);
        given(memberStampRepository.save(any(MemberStamp.class))).willReturn(memberStamp);
        given(stationRepository.findById(stationId)).willReturn(Optional.of(station));

        // when
        CourseCompleteResponse result = stampCommandService.completeCourse(memberId, courseId);

        // then
        assertThat(result.memberStampId()).isEqualTo(501L);
        assertThat(result.stationId()).isEqualTo(stationId);
        assertThat(result.stationName()).isEqualTo("보문역");
        assertThat(result.acquiredAt()).isEqualTo(LocalDateTime.of(2026, 7, 7, 18, 30));
        verify(memberStampRepository).save(any(MemberStamp.class));
    }

    @Test
    @DisplayName("본인 코스가 아니면 COURSE_NOT_OWNED 예외가 발생한다")
    void completeCourse_notOwned() {
        // given
        Long memberId = 1L;
        Long courseId = 10L;
        Long otherMemberId = 2L;

        CourseInfoResponse courseInfo = new CourseInfoResponse(
                courseId, "보문역 코스", otherMemberId, 12L, null, 0, 0, LocalDateTime.now()
        );

        given(courseQueryService.getCourseInfo(courseId)).willReturn(courseInfo);

        // when & then
        assertThatThrownBy(() -> stampCommandService.completeCourse(memberId, courseId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(StampErrorCode.COURSE_NOT_OWNED.getMessage());

        verify(memberStampRepository, never()).save(any());
    }

    @Test
    @DisplayName("역이 존재하지 않으면 STATION_NOT_FOUND 예외가 발생한다")
    void completeCourse_stationNotFound() {
        // given
        Long memberId = 1L;
        Long courseId = 10L;
        Long stationId = 999L;

        CourseInfoResponse courseInfo = new CourseInfoResponse(
                courseId, "보문역 코스", memberId, stationId, null, 0, 0, LocalDateTime.now()
        );

        MemberStamp memberStamp = mock(MemberStamp.class);

        given(courseQueryService.getCourseInfo(courseId)).willReturn(courseInfo);
        given(memberStampRepository.save(any(MemberStamp.class))).willReturn(memberStamp);
        given(stationRepository.findById(stationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> stampCommandService.completeCourse(memberId, courseId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(StampErrorCode.STATION_NOT_FOUND.getMessage());
    }
}