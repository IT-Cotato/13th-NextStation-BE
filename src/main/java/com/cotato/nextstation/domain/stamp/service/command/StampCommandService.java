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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StampCommandService {

    private final CourseQueryService courseQueryService;
    private final MemberStampRepository memberStampRepository;
    private final StationRepository stationRepository;

    public CourseCompleteResponse completeCourse(Long memberId, Long courseId) {
        CourseInfoResponse courseInfo = courseQueryService.getCourseInfo(courseId);

        // 본인 코스인지 검증
        if (!courseInfo.memberId().equals(memberId)) {
            log.warn("본인 코스가 아닌 완료 요청: memberId={}, courseOwnerId={}, courseId={}",
                    memberId, courseInfo.memberId(), courseId);
            throw new CustomException(StampErrorCode.COURSE_NOT_OWNED);
        }

        MemberStamp memberStamp = memberStampRepository.save(
                MemberStamp.builder()
                        .memberId(memberId)
                        .courseId(courseId)
                        .build()
        );

        Station station = stationRepository.findById(courseInfo.stationId())
                .orElseThrow(() -> new CustomException(StampErrorCode.STATION_NOT_FOUND));

        log.info("코스 완료 처리: memberId={}, courseId={}, memberStampId={}",
                memberId, courseId, memberStamp.getId());

        return new CourseCompleteResponse(
                memberStamp.getId(),
                station.getId(),
                station.getStationName(),
                memberStamp.getCreatedAt()
        );
    }
}