package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.response.ConceptTourResponse;
import com.cotato.nextstation.domain.course.service.query.ConceptTourQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 컨셉투어는 코스를 묶는 분류라 Course 도메인이 소유한다. Swagger에서도 같은 섹션에 둔다.
@Tag(name = "Course")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/concept-tours")
public class ConceptTourController {

    private final ConceptTourQueryService conceptTourQueryService;

    @Operation(
            summary = "컨셉별 투어 목록 조회",
            description = """
                    둘러보기의 "컨셉별 투어"에 쓴다. 관리자가 정한 표시 순서대로 전부 내려준다.
                    - 컨셉이 여덟 개 남짓이라 **페이징하지 않는다.**
                    - 화면의 검색창은 이 목록을 받아 프론트에서 걸러내면 된다. 서버는 검색어를 받지 않는다.
                    - `courseCount`는 목록에 실제로 보이는 것과 같도록 **공개된 코스만** 센다.
                    - 로그인 없이 조회할 수 있다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (컨셉이 없으면 빈 목록)"),
    })
    @GetMapping
    public CommonResponse<List<ConceptTourResponse>> getConceptTours() {
        return CommonResponse.success(conceptTourQueryService.getConceptTours());
    }
}
