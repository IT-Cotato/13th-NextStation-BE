package com.cotato.nextstation.domain.place.init;

import com.cotato.nextstation.domain.place.repository.PlaceTagMappingRepository;
import com.cotato.nextstation.domain.place.repository.PlaceTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 기획 변경으로 폐지된 place_tag(ART_SPACE/LOCAL_EXPLORE/CAFE_TOUR)를 정리한다.
 * PlaceSeeder가 이미 실행돼 place_tag_mapping이 이 태그들을 FK로 참조하고 있는 로컬 환경도 있어 매핑을 먼저 지우고 태그를 지운다.
 * enum에서도 상수를 제거했으므로 이름은 문자열로 다룬다.
 * 대상이 없으면 아무것도 지우지 않는 멱등 작업이라 재시작마다 실행해도 안전하다.
 */
@Slf4j
@Component
@Profile("!prod")
@Order(4)
@RequiredArgsConstructor
public class RetiredPlaceTagCleaner implements ApplicationRunner {

    private static final List<String> RETIRED_TAG_NAMES = List.of("ART_SPACE", "LOCAL_EXPLORE", "CAFE_TOUR");

    private final PlaceTagMappingRepository placeTagMappingRepository;
    private final PlaceTagRepository placeTagRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        placeTagMappingRepository.deleteByPlaceTagNames(RETIRED_TAG_NAMES);
        placeTagRepository.deleteByNames(RETIRED_TAG_NAMES);
        log.info("폐지된 place_tag 정리 완료: names={}", RETIRED_TAG_NAMES);
    }
}