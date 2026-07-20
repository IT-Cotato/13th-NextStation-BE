package com.cotato.nextstation.domain.place.service.query;

import com.cotato.nextstation.domain.place.converter.PlaceConverter;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.dto.response.StationTagCountResponse;
import com.cotato.nextstation.domain.place.entity.Place;
import com.cotato.nextstation.domain.place.entity.PlaceTagMapping;
import com.cotato.nextstation.domain.place.enums.PlaceTagName;
import com.cotato.nextstation.domain.place.repository.PlaceRepository;
import com.cotato.nextstation.domain.place.repository.PlaceTagMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceInfoQueryService {

    private static final int TOP_TAG_LIMIT = 3;

    private final PlaceRepository placeRepository;
    private final PlaceTagMappingRepository placeTagMappingRepository;
    private final PlaceConverter placeConverter;

    public List<PlaceInfoResponse> getPlaceInfos(List<Long> placeIds) {
        List<Place> places = placeRepository.findAllById(placeIds);
        return placeConverter.toPlaceInfoResponses(places);
    }

    public List<String> getTopTagNames(List<Long> placeIds) {
        List<PlaceTagMapping> mappings = placeTagMappingRepository.findByPlaceIdIn(placeIds);

        Map<String, Long> tagCountMap = mappings.stream()
                .collect(Collectors.groupingBy(
                        mapping -> mapping.getPlaceTag().getName().name(),
                        Collectors.counting()
                ));

        return tagCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_TAG_LIMIT)
                .map(Map.Entry::getKey)
                .toList();
    }

    public StationTagCountResponse getPlaceCountsByStationForTags(List<String> tags) {
        List<PlaceTagName> tagNames = tags.stream()
                .map(PlaceTagName::valueOf)
                .toList();

        List<PlaceTagMapping> mappings = placeTagMappingRepository.findByPlaceTagNameIn(tagNames);

        Map<Long, Map<String, Long>> result = mappings.stream()
                .collect(Collectors.groupingBy(
                        mapping -> mapping.getPlace().getStationId(),
                        Collectors.groupingBy(
                                mapping -> mapping.getPlaceTag().getName().name(),
                                Collectors.counting()
                        )
                ));

        return new StationTagCountResponse(result);
    }

}