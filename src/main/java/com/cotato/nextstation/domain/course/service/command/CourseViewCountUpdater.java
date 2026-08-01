package com.cotato.nextstation.domain.course.service.command;

import com.cotato.nextstation.domain.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 코스 조회수 증가만 담당한다.
 * <p>
 * 조회수를 올리는 곳은 코스 상세 조회인데, 그 조회 서비스는 {@code @Transactional(readOnly = true)}라
 * 같은 트랜잭션에서 UPDATE를 할 수 없다. 그래서 {@link Propagation#REQUIRES_NEW}로 별도 트랜잭션을 연다.
 * <p>
 * 별도 클래스로 뺀 이유는 프록시 때문이다. 같은 빈 안에서 호출하면 트랜잭션 프록시를 타지 않아
 * REQUIRES_NEW가 적용되지 않는다.
 * <p>
 * 증가 후 최신 조회수를 반환한다. 호출부의 바깥 트랜잭션에서 다시 조회하면 REPEATABLE READ
 * 스냅샷 때문에 증가 전 값을 보게 되므로, 이 값을 그대로 받아써야 한다.
 */
@Component
@RequiredArgsConstructor
public class CourseViewCountUpdater {

    private final CourseRepository courseRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int increaseViewCount(Long courseId, Long viewerMemberId) {
        courseRepository.increaseViewCount(courseId, viewerMemberId);
        return courseRepository.findViewCountById(courseId);
    }
}
