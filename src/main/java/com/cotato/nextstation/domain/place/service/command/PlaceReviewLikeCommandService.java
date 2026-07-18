package com.cotato.nextstation.domain.place.service.command;

import com.cotato.nextstation.domain.place.converter.PlaceReviewLikeConverter;
import com.cotato.nextstation.domain.place.dto.response.PlaceReviewLikeResponse;
import com.cotato.nextstation.domain.place.entity.PlaceReview;
import com.cotato.nextstation.domain.place.entity.PlaceReviewLike;
import com.cotato.nextstation.domain.place.exception.PlaceReviewErrorCode;
import com.cotato.nextstation.domain.place.repository.PlaceReviewLikeRepository;
import com.cotato.nextstation.domain.place.repository.PlaceReviewRepository;
import com.cotato.nextstation.global.exception.CustomException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PlaceReviewLikeCommandService {

    private final PlaceReviewRepository placeReviewRepository;
    private final PlaceReviewLikeRepository placeReviewLikeRepository;
    private final PlaceReviewLikeConverter placeReviewLikeConverter;
    private final EntityManager entityManager;

    public PlaceReviewLikeResponse like(Long memberId, Long reviewId) {
        PlaceReview review = findReview(reviewId);

        if (placeReviewLikeRepository.existsByMemberIdAndPlaceReview(memberId, review)) {
            throw new CustomException(PlaceReviewErrorCode.PLACE_REVIEW_LIKE_ALREADY_EXISTS);
        }

        try {
        placeReviewLikeRepository.save(PlaceReviewLike.builder()
                .memberId(memberId)
                .placeReview(review)
                .build());
        } catch (DataIntegrityViolationException e) {
            // 애플리케이션 레벨 중복 체크 사이의 레이스 컨디션을 DB UNIQUE 제약이 잡아낸 경우
            throw new CustomException(PlaceReviewErrorCode.PLACE_REVIEW_LIKE_ALREADY_EXISTS);
        }
        placeReviewRepository.incrementLikeCount(reviewId);

        long freshCount = refetchLikeCount(review);
        return placeReviewLikeConverter.toLikeResponse(reviewId, freshCount, true);
    }

    public PlaceReviewLikeResponse unlike(Long memberId, Long reviewId) {
        PlaceReview review = findReview(reviewId);

        PlaceReviewLike like = placeReviewLikeRepository.findByMemberIdAndPlaceReview(memberId, review)
                .orElseThrow(() -> new CustomException(PlaceReviewErrorCode.PLACE_REVIEW_LIKE_NOT_FOUND));

        placeReviewLikeRepository.delete(like);
        placeReviewRepository.decrementLikeCount(reviewId);

        long freshCount = refetchLikeCount(review);
        return placeReviewLikeConverter.toLikeResponse(reviewId, freshCount, false);
    }

    private PlaceReview findReview(Long reviewId) {
        return placeReviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(PlaceReviewErrorCode.PLACE_REVIEW_NOT_FOUND));
    }

    // 벌크 UPDATE로 변경된 like_count를 영속성 컨텍스트가 인지하지 못하므로,
    // 해당 엔티티만 DB에서 강제로 다시 읽어와 최신 값을 보장한다.
    private long refetchLikeCount(PlaceReview review) {
        entityManager.refresh(review);
        return review.getLikeCount();
    }
}