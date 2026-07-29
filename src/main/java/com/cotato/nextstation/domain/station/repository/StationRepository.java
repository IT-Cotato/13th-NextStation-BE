package com.cotato.nextstation.domain.station.repository;

import com.cotato.nextstation.domain.station.entity.Station;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {

    Optional<Station> findByStationName(String stationName);

    /**
     * 역 검색(부분일치). "십리"로 왕십리역·답십리역·상왕십리역이 모두 걸린다.
     * <p>
     * 서울 내 역 308개가 예외 없이 "역"으로 끝나 꼬리의 "역"은 검색어로서 변별력이 없다.
     * 그래서 역명에서도 떼고 비교한다(검색어 쪽은 서비스에서 뗀다).
     * <p>
     * 정렬은 완전일치 → 접두사일치 → 나머지 포함 순이다. 역명순만 쓰면
     * "서울역"을 검색해도 서울대입구역이 서울역보다 위에 나온다.
     * <p>
     * {@code pattern}은 LIKE 와일드카드를 이스케이프한 값이고 {@code keyword}는 원본이다.
     * 완전일치 비교(=)에는 이스케이프하지 않은 원본을 써야 한다.
     */
    @Query("""
            SELECT s FROM Station s
            WHERE TRIM(TRAILING '역' FROM s.stationName) LIKE CONCAT('%', :pattern, '%') ESCAPE '!'
            ORDER BY
                CASE
                    WHEN TRIM(TRAILING '역' FROM s.stationName) = :keyword THEN 0
                    WHEN TRIM(TRAILING '역' FROM s.stationName) LIKE CONCAT(:pattern, '%') ESCAPE '!' THEN 1
                    ELSE 2
                END,
                s.stationName ASC
            """)
    List<Station> searchByNormalizedName(@Param("keyword") String keyword,
                                         @Param("pattern") String pattern,
                                         Pageable pageable);

    // 랜덤뽑기 대상(뽑기 역 50개) 조회
    List<Station> findByIsDrawableTrue();
}
