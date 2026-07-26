package com.cotato.nextstation.domain.auth.client;

import com.cotato.nextstation.domain.auth.client.dto.KakaoTokenResponse;
import com.cotato.nextstation.domain.auth.client.dto.KakaoUserInfoResponse;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

// 카카오 OAuth 서버(kauth/kapi.kakao.com)와 직접 통신하는 클라이언트: 인가코드 교환 + 사용자 정보 조회
@Slf4j
@Component
public class KakaoOAuthClient {

    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoOAuthClient(@Value("${kakao.oauth.client-id}") String clientId,
                             @Value("${kakao.oauth.client-secret:}") String clientSecret,
                             @Value("${kakao.oauth.redirect-uri}") String redirectUri) {
        
        // RestClientAutoConfiguration이 RestClient.Builder 빈을 안 만들어줘서 직접 생성
        this.restClient = RestClient.builder().build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    // code는 1회용/단기 만료라 재사용 시 카카오가 4xx를 반환한다
    public KakaoTokenResponse exchangeToken(String code) {

        // 카카오 토큰 엔드포인트는 JSON이 아니라 form-urlencoded로 받는다
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);

        if (!clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }

        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        try {
            return restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

        } catch (RestClientResponseException e) {

            if (e.getStatusCode().is4xxClientError()) {
                log.warn("카카오 토큰 교환 실패(4xx): status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new CustomException(AuthErrorCode.INVALID_KAKAO_CODE);
            }
            log.warn("카카오 토큰 교환 실패(5xx)", e);
            throw new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR);

        } catch (RestClientException e) {
            log.warn("카카오 토큰 교환 중 통신 오류", e);
            throw new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR);
        }
    }

    public KakaoUserInfoResponse fetchUserInfo(String kakaoAccessToken) {
        try {
            return restClient.get()
                    .uri(USER_INFO_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

        } catch (RestClientException e) {
            log.warn("카카오 사용자 정보 조회 실패", e);
            throw new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR);
        }
    }
}