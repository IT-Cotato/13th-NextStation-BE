package com.cotato.nextstation.domain.stamp.converter;

import com.cotato.nextstation.domain.course.dto.response.PopularCourseResponse;
import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.stamp.dto.response.StationPopularCoursesResponse;
import com.cotato.nextstation.domain.station.entity.Station;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class StampCourseConverter {

    public StationPopularCoursesResponse toStationPopularCoursesResponse(
            Station station,
            String lineName,
            List<PopularCourseResponse> courses) {
        return new StationPopularCoursesResponse(station.getStationName(), lineName, courses);
    }

}