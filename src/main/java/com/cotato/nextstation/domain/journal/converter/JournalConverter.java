package com.cotato.nextstation.domain.journal.converter;

import com.cotato.nextstation.domain.course.dto.response.CourseInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceInfoResponse;
import com.cotato.nextstation.domain.journal.dto.response.JournalDetailResponse;
import com.cotato.nextstation.domain.journal.dto.response.JournalWriteInfoResponse;
import com.cotato.nextstation.domain.journal.dto.response.UncompletedJournalListResponse;
import com.cotato.nextstation.domain.journal.entity.Journal;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.entity.PlaceReview;
import com.cotato.nextstation.domain.stamp.entity.MemberStamp;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JournalConverter {

    public JournalWriteInfoResponse toWriteInfoResponse(
            String stationName,
            String courseName,
            List<String> tags,
            List<CoursePlaceInfoResponse> coursePlaces,
            Map<Long, PlaceInfoResponse> placeInfoMap
    ) {
        List<JournalWriteInfoResponse.PlaceSimpleResponse> places = coursePlaces.stream()
                .map(cp -> {
                    PlaceInfoResponse info = placeInfoMap.get(cp.placeId());
                    String placeName = info != null ? info.placeName() : null;

                    return new JournalWriteInfoResponse.PlaceSimpleResponse(cp.placeId(),
                            placeName,
                            cp.orderNum()
                    );
                } )
                .toList();

        return new JournalWriteInfoResponse(stationName, courseName, tags, places);
    }

        public UncompletedJournalListResponse toUncompletedJournalListResponse(
                List<MemberStamp> uncompletedStamps,
                Map<Long, CourseInfoResponse> courseInfoMap,
                Map<Long, String> stationNameMap,
                Map<Long, List<String>> tagsByCourse
        ) {
            List<UncompletedJournalListResponse.UncompletedCourseResponse> courses =
                    uncompletedStamps.stream()
                            .map(stamp -> {
                                CourseInfoResponse courseInfo = courseInfoMap.get(stamp.getCourseId());
                                String stationName = stationNameMap.get(courseInfo.stationId());
                                List<String> tags = tagsByCourse.get(stamp.getCourseId());

                                return new UncompletedJournalListResponse.UncompletedCourseResponse(
                                        stamp.getId(),
                                        stationName,
                                        courseInfo.name(),
                                        tags,
                                        stamp.getCreatedAt()
                                );
                            })
                            .toList();

            return new UncompletedJournalListResponse(courses.size(), courses);
        }

    public JournalDetailResponse toJournalDetailResponse(
            Journal journal,
            LineSummaryResponse line,
            String stationName,
            CourseInfoResponse courseInfo,
            int viewCount,
            boolean isMine,
            boolean isLiked,
            List<String> tags,
            List<String> imageUrls,
            List<CoursePlaceInfoResponse> coursePlaces,
            Map<Long, PlaceInfoResponse> placeInfoMap,
            Map<Long, PlaceReview> reviewByPlaceId,
            Map<Long, String> imageUrlByReviewId
    ) {
        String profileImageUrl = journal.getMember().getProfileImageUrl();
        String writerProfileImageUrl = (profileImageUrl == null || profileImageUrl.isBlank())
                ? null : profileImageUrl;

        List<JournalDetailResponse.VisitedPlaceResponse> visitedPlaces =
                toVisitedPlaceResponses(coursePlaces, placeInfoMap, reviewByPlaceId, imageUrlByReviewId);

        return new JournalDetailResponse(
                journal.getMember().getNickname(),
                writerProfileImageUrl,
                journal.getTraveledAt(),
                line,
                stationName,
                courseInfo.courseId(),
                courseInfo.name(),
                isMine,
                isLiked,
                tags,
                journal.getTravelDuration(),
                viewCount,
                courseInfo.likeCount(),
                imageUrls,
                journal.getOverallReview(),
                visitedPlaces
        );
    }

    private List<JournalDetailResponse.VisitedPlaceResponse> toVisitedPlaceResponses(
            List<CoursePlaceInfoResponse> coursePlaces,
            Map<Long, PlaceInfoResponse> placeInfoMap,
            Map<Long, PlaceReview> reviewByPlaceId,
            Map<Long, String> imageUrlByReviewId
    ) {
        return coursePlaces.stream()
                .map(cp -> {
                    PlaceInfoResponse placeInfo = placeInfoMap.get(cp.placeId());
                    PlaceReview review = reviewByPlaceId.get(cp.placeId());
                    String reviewImageUrl = review != null
                            ? imageUrlByReviewId.get(review.getId())
                            : null;

                    return new JournalDetailResponse.VisitedPlaceResponse(
                            cp.orderNum(),
                            cp.placeId(),
                            placeInfo != null ? placeInfo.placeName() : null,
                            placeInfo != null ? placeInfo.xCoordinate() : null,
                            placeInfo != null ? placeInfo.yCoordinate() : null,
                            review != null ? review.getReview() : null,
                            reviewImageUrl
                    );
                })
                .toList();
    }
    }