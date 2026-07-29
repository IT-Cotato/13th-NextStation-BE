package com.cotato.nextstation.domain.journal.service.query;

import com.cotato.nextstation.domain.course.dto.response.CourseInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceInfoResponse;
import com.cotato.nextstation.domain.course.service.query.CourseQueryService;
import com.cotato.nextstation.domain.journal.dto.response.JournalWriteInfoResponse;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.service.query.PlaceInfoQueryService;
import com.cotato.nextstation.domain.stamp.service.query.MemberStampQueryService;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalQueryService {

    private final MemberStampQueryService memberStampQueryService;
    private final CourseQueryService courseQueryService;
    private final PlaceInfoQueryService placeInfoQueryService;
    private final StationRepository stationRepository;

    public JournalWriteInfoResponse getWriteInfo(Long memberId, Long memberStampId) {
        // 1. memberStampId → courseId
        Long courseId = memberStampQueryService.getCourseId(memberId, memberStampId);

        // 2. courseId → 코스 정보 (courseName, stationId)
        CourseInfoResponse courseInfo = courseQueryService.getCourseInfo(courseId);

        // 3. stationId → stationName
        String stationName = stationRepository.findById(courseInfo.stationId())
                .map(station -> station.getStationName())
                .orElse(null);

        // 4. courseId → 장소 목록 (placeId + orderNum)
        List<CoursePlaceInfoResponse> coursePlaces = courseQueryService.getCoursePlaces(courseId);
        List<Long> placeIds = coursePlaces.stream()
                .map(CoursePlaceInfoResponse::placeId)
                .toList();

        // 5. placeIds → 장소 이름
        Map<Long, PlaceInfoResponse> placeInfoMap = placeInfoQueryService.getPlaceInfos(placeIds)
                .stream()
                .collect(Collectors.toMap(PlaceInfoResponse::placeId, Function.identity()));

        // 6. placeIds → 태그 상위 3개
        List<String> tags = placeInfoQueryService.getTopTagNames(placeIds);

        return JournalWriteInfoResponse.of(stationName, courseInfo.name(), tags, coursePlaces, placeInfoMap);
    }
}