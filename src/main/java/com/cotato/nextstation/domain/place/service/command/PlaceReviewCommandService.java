package com.cotato.nextstation.domain.place.service.command;

import com.cotato.nextstation.domain.journal.entity.Journal;
import com.cotato.nextstation.domain.place.dto.request.PlaceReviewCreateRequest;
import com.cotato.nextstation.domain.place.entity.Place;
import com.cotato.nextstation.domain.place.entity.PlaceReview;
import com.cotato.nextstation.domain.place.entity.PlaceReviewImage;
import com.cotato.nextstation.domain.place.exception.PlaceErrorCode;
import com.cotato.nextstation.domain.place.repository.PlaceRepository;
import com.cotato.nextstation.domain.place.repository.PlaceReviewImageRepository;
import com.cotato.nextstation.domain.place.repository.PlaceReviewRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;



/**
 * 장소 리뷰 쓰기 전용 서비스.
 *
 * 여행일지 작성(journal 도메인)은 장소 리뷰(place 도메인)를 함께 저장해야 한다.
 * 이때 JournalCommandService가 PlaceReviewRepository 등 place 도메인의 Repository를
 * 직접 참조하면, place 도메인 내부 구조 변경이 journal 도메인에도 영향을 미친다.
 *
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlaceReviewCommandService {

    private final PlaceRepository placeRepository;
    private final PlaceReviewRepository placeReviewRepository;
    private final PlaceReviewImageRepository placeReviewImageRepository;


    // 여행일지 작성 시 장소 리뷰 일괄 저장
    public void createPlaceReviews(Journal journal, List<PlaceReviewCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        requests.forEach(request -> {
            Place place = placeRepository.findById(request.placeId())
                    .orElseThrow(() -> new CustomException(PlaceErrorCode.PLACE_NOT_FOUND));

            PlaceReview placeReview = PlaceReview.builder()
                    .place(place)
                    .journal(journal)
                    .review(request.review())
                    .build();
            placeReviewRepository.save(placeReview);

            if (request.imageUrl() != null) {
                placeReviewImageRepository.save(
                        PlaceReviewImage.builder()
                                .placeReview(placeReview)
                                .imageUrl(request.imageUrl())
                                .build()
                );
            }
        });
    }
}