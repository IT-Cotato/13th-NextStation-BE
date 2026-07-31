package com.cotato.nextstation.domain.journal.service.query;

import com.cotato.nextstation.domain.journal.dto.response.JournalCardInfoResponse;
import com.cotato.nextstation.domain.journal.entity.Journal;
import com.cotato.nextstation.domain.journal.entity.JournalImage;
import com.cotato.nextstation.domain.journal.repository.JournalImageRepository;
import com.cotato.nextstation.domain.journal.repository.JournalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 코스 카드에 얹을 여행일지 정보(대표 사진·소요시간)만 조회한다.
 * <p>
 * {@code JournalQueryService}에 두지 않은 이유는 순환 참조 때문이다. 여행일지 상세는 코스 정보를
 * 받아와야 해서 {@code CourseQueryService}를 주입받는데, 코스 카드 쪽은 반대로 여행일지 정보가
 * 필요하다. 한 클래스에 두면 두 서비스가 서로를 참조해 애플리케이션이 기동되지 않는다.
 * <p>
 * 이 클래스는 여행일지 저장소만 보고 다른 도메인을 참조하지 않으므로 어느 쪽에서든 부를 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalCardQueryService {

    private final JournalRepository journalRepository;
    private final JournalImageRepository journalImageRepository;

    /**
     * 코스 카드 한 장에 필요한 여행일지 정보. 일지가 없으면 비어 있다.
     */
    public Optional<JournalCardInfoResponse> getJournalCourseCardInfo(Long journalId) {
        if (journalId == null) {
            return Optional.empty();
        }
        return journalRepository.findById(journalId)
                .map(journal -> new JournalCardInfoResponse(
                        journal.getId(),
                        journalImageRepository.findFirstByJournalIdOrderByCreatedAtAsc(journalId)
                                .map(JournalImage::getImageUrl)
                                .orElse(null),
                        journal.getTravelDuration()));
    }

    /**
     * 카드가 여러 개인 화면(둘러보기 목록·장소별 코스)용. 일지와 사진을 각각 한 번씩만 읽는다.
     * <p>
     * 단건 메서드를 카드마다 부르면 카드 수만큼 쿼리가 나간다. 일지별 첫 사진은 DB에서 자르지 않고
     * 정렬해 받아 온 뒤 메모리에서 고르므로, 사진이 여러 장이어도 쿼리가 늘지 않는다.
     * <p>
     * 일지가 없는 id는 결과에서 빠진다. null은 무시한다.
     */
    public Map<Long, JournalCardInfoResponse> getJournalCourseCardInfos(List<Long> journalIds) {
        List<Long> targets = journalIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (targets.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> firstImageByJournal = journalImageRepository
                .findImagesByJournalIds(targets).stream()
                .collect(Collectors.toMap(
                        JournalImageRepository.JournalImageView::getJournalId,
                        JournalImageRepository.JournalImageView::getImageUrl,
                        (first, next) -> first));

        return journalRepository.findAllById(targets).stream()
                .collect(Collectors.toMap(
                        Journal::getId,
                        journal -> new JournalCardInfoResponse(
                                journal.getId(),
                                firstImageByJournal.get(journal.getId()),
                                journal.getTravelDuration())));
    }
}
