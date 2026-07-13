package com.cotato.nextstation.global.exception;

import com.cotato.nextstation.global.exception.support.ExceptionTestController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExceptionTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void customException_는_에러코드에_매핑된_상태코드를_반환한다() throws Exception {
        mockMvc.perform(get("/test/custom"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_404_NOT_FOUND"));
    }

    @Test
    void 잘못된_JSON_본문은_500이_아닌_400을_반환한다() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-a-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_INVALID_REQUEST"));
    }

    @Test
    void validation_필드_에러는_reasons에_담겨_400을_반환한다() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.name").exists());
    }

    @Test
    void 필수_파라미터_누락은_500이_아닌_400을_반환한다() throws Exception {
        mockMvc.perform(get("/test/required-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_INVALID_REQUEST"));
    }

    @Test
    void 파라미터_타입_불일치는_500이_아닌_400을_반환한다() throws Exception {
        mockMvc.perform(get("/test/type-mismatch").param("number", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_INVALID_REQUEST"));
    }

    @Test
    void 지원하지_않는_HTTP_메서드는_500이_아닌_405를_반환한다() throws Exception {
        mockMvc.perform(put("/test/custom"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_405_METHOD_NOT_ALLOWED"));
    }
}