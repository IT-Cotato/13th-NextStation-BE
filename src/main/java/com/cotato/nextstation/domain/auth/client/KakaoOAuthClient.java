package com.cotato.nextstation.domain.auth.client;

import com.cotato.nextstation.domain.auth.client.dto.KakaoErrorResponse;
import com.cotato.nextstation.domain.auth.client.dto.KakaoTokenResponse;
import com.cotato.nextstation.domain.auth.client.dto.KakaoUserInfoResponse;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Set;

// 카카오 OAuth 서버(kauth/kapi.kakao.com)와 직접 통신하는 클라이언트: 인가코드 교환 + 사용자 정보 조회
@Slf4j
@Component
public class KakaoOAuthClient {

    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    // 사용자의 인가코드 문제가 아니라 우리 앱의 카카오 콘솔 설정/연동 문제로 봐야 하는 error_code
    private static final Set<String> MISCONFIGURATION_ERROR_CODES = Set.of("KOE303", "KOE237");

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoOAuthClient(@Value("${kakao.oauth.client-id}") String clientId,
                             @Value("${kakao.oauth.client-secret:}") String clientSecret,
                             @Value("${kakao.oauth.redirect-uri}") String redirectUri) {

        // RestClientAutoConfiguration이 RestClient.Builder 빈을 안 만들어줘서 직접 생성.
        // 기본 factory는 타임아웃이 사실상 무제한이라, 카카오 응답이 느려지면 요청 스레드가 오래 붙잡힐 수 있어 명시적으로 설정한다.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
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
                throw mapTokenExchange4xx(e);
            }
            log.warn("카카오 토큰 교환 실패(5xx): status={}", e.getStatusCode());
            throw new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR);

        } catch (RestClientException e) {
            log.warn("카카오 토큰 교환 중 통신 오류", e);
            throw new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR);
        }
    }

    // 카카오 응답의 error/error_code로 "사용자 인가코드 문제(재사용·만료 등)"와 "우리 앱 설정/연동 문제"를 구분한다.
    // error_description은 인가코드 원문을 그대로 담고 있는 경우가 있어 절대 로그에 남기지 않는다.
    private CustomException mapTokenExchange4xx(RestClientResponseException e) {
        KakaoErrorResponse error = parseErrorBody(e);

        if (error == null) {
            log.warn("카카오 토큰 교환 실패(4xx, 본문 파싱 불가): status={}", e.getStatusCode());
            return new CustomException(AuthErrorCode.INVALID_KAKAO_CODE);
        }

        if ("invalid_client".equals(error.error()) || MISCONFIGURATION_ERROR_CODES.contains(error.errorCode())) {
            log.error("카카오 OAuth 설정/연동 오류: status={}, error={}, errorCode={}",
                    e.getStatusCode(), error.error(), error.errorCode());
            return new CustomException(AuthErrorCode.KAKAO_OAUTH_MISCONFIGURED);
        }

        log.warn("카카오 토큰 교환 실패(4xx): status={}, error={}, errorCode={}",
                e.getStatusCode(), error.error(), error.errorCode());
        return new CustomException(AuthErrorCode.INVALID_KAKAO_CODE);
    }

    private KakaoErrorResponse parseErrorBody(RestClientResponseException e) {
        try {
            return e.getResponseBodyAs(KakaoErrorResponse.class);
        } catch (Exception parseException) {
            return null;
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