package com.cotato.nextstation.domain.place.service.command;

import com.cotato.nextstation.domain.journal.entity.Journal;
import com.cotato.nextstation.domain.journal.enums.ImageAction;
import com.cotato.nextstation.domain.place.dto.request.PlaceReviewCreateRequest;
import com.cotato.nextstation.domain.place.dto.request.PlaceReviewUpdateRequest;
import com.cotato.nextstation.domain.place.entity.Place;
import com.cotato.nextstation.domain.place.entity.PlaceReview;
import com.cotato.nextstation.domain.place.repository.PlaceRepository;
import com.cotato.nextstation.domain.place.repository.PlaceReviewImageRepository;
import com.cotato.nextstation.domain.place.repository.PlaceReviewLikeRepository;
import com.cotato.nextstation.domain.place.repository.PlaceReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// https://github.com/IT-Cotato/13th-NextStation-BE/issues/172
// 리뷰 텍스트/사진이 모두 없으면 리뷰 자체를 만들지(또는 유지하지) 않도록 하는 정책을 검증한다.
@ExtendWith(MockitoExtension.class)
class PlaceReviewCommandServiceTest {

    @InjectMocks
    private PlaceReviewCommandService placeReviewCommandService;

    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private PlaceReviewRepository placeReviewRepository;
    @Mock
    private PlaceReviewImageRepository placeReviewImageRepository;
    @Mock
    private PlaceReviewLikeRepository placeReviewLikeRepository;

    private static final Long PLACE_ID = 10L;

    @Nested
    @DisplayName("createPlaceReviews")
    class CreatePlaceReviews {

        @Test
        @DisplayName("텍스트와 사진이 모두 없는 요청은 저장하지 않는다")
        void skipsRequestWithoutTextAndImage() {
            // given
            Journal journal = mock(Journal.class);
            List<PlaceReviewCreateRequest> requests = List.of(
                    new PlaceReviewCreateRequest(PLACE_ID, null, null),
                    new PlaceReviewCreateRequest(PLACE_ID, "  ", null)
            );

            // when
            placeReviewCommandService.createPlaceReviews(journal, requests);

            // then
            verify(placeRepository, never()).findAllById(anyList());
            verify(placeReviewRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("텍스트 없이 사진만 있어도 리뷰로 저장한다")
        void savesImageOnlyRequest() {
            // given
            Journal journal = mock(Journal.class);
            Place place = mock(Place.class);
            given(place.getId()).willReturn(PLACE_ID);
            given(placeRepository.findAllById(List.of(PLACE_ID))).willReturn(List.of(place));

            PlaceReview saved = PlaceReview.builder().place(place).journal(journal).review(null).build();
            given(placeReviewRepository.saveAll(anyList())).willReturn(List.of(saved));

            List<PlaceReviewCreateRequest> requests = List.of(
                    new PlaceReviewCreateRequest(PLACE_ID, null, "https://image.example/1.jpg")
            );

            // when
            placeReviewCommandService.createPlaceReviews(journal, requests);

            // then
            ArgumentCaptor<List<PlaceReview>> captor = ArgumentCaptor.forClass(List.class);
            verify(placeReviewRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).getReview()).isNull();
            verify(placeReviewImageRepository).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("updatePlaceReviews")
    class UpdatePlaceReviews {

        @Test
        @DisplayName("수정 결과 텍스트와 사진이 모두 없으면 리뷰를 소프트 삭제하고 걸려있던 좋아요도 정리한다")
        void softDeletesWhenTextAndImageBothCleared() {
            // given
            Journal journal = mock(Journal.class);
            given(journal.getId()).willReturn(1L);

            Place place = mock(Place.class);
            PlaceReview placeReview = PlaceReview.builder().place(place).journal(journal).review("원래 리뷰").build();
            given(placeReviewRepository.findByJournalIdAndPlaceId(1L, PLACE_ID)).willReturn(Optional.of(placeReview));
            given(placeReviewImageRepository.findByPlaceReview(placeReview)).willReturn(List.of());

            List<PlaceReviewUpdateRequest> requests = List.of(
                    new PlaceReviewUpdateRequest(PLACE_ID, null, ImageAction.KEEP, null)
            );

            // when
            placeReviewCommandService.updatePlaceReviews(journal, requests);

            // then
            assertThat(placeReview.isDeleted()).isTrue();
            assertThat(placeReview.getLikeCount()).isZero();
            verify(placeReviewLikeRepository).deleteByPlaceReview(placeReview);
        }

        @Test
        @DisplayName("텍스트가 남아 있으면 사진을 지워도 소프트 삭제되지 않는다")
        void keepsReviewWhenTextRemains() {
            // given
            Journal journal = mock(Journal.class);
            given(journal.getId()).willReturn(1L);

            Place place = mock(Place.class);
            PlaceReview placeReview = PlaceReview.builder().place(place).journal(journal).review("원래 리뷰").build();
            given(placeReviewRepository.findByJournalIdAndPlaceId(1L, PLACE_ID)).willReturn(Optional.of(placeReview));

            List<PlaceReviewUpdateRequest> requests = List.of(
                    new PlaceReviewUpdateRequest(PLACE_ID, "여전히 좋았어요", ImageAction.DELETE, null)
            );

            // when
            placeReviewCommandService.updatePlaceReviews(journal, requests);

            // then
            assertThat(placeReview.isDeleted()).isFalse();
            assertThat(placeReview.getReview()).isEqualTo("여전히 좋았어요");
            verify(placeReviewLikeRepository, never()).deleteByPlaceReview(placeReview);
        }
    }
}
