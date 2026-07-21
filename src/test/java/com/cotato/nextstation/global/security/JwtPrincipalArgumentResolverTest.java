package com.cotato.nextstation.global.security;

import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import com.cotato.nextstation.global.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JwtPrincipalArgumentResolverTest {

    @InjectMocks
    private JwtPrincipalArgumentResolver resolver;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private NativeWebRequest webRequest;

    private static final String TOKEN = "access-token";
    private static final String AUTH_HEADER = "Bearer " + TOKEN;

    // 리졸버 대상 파라미터를 만들기 위한 더미 메서드
    void dummyController(@AuthenticationPrincipal JwtPrincipal principal) {
    }

    void dummyControllerWithoutAnnotation(JwtPrincipal principal) {
    }

    private MethodParameter annotatedParameter() throws NoSuchMethodException {
        return new MethodParameter(
                JwtPrincipalArgumentResolverTest.class.getDeclaredMethod("dummyController", JwtPrincipal.class), 0);
    }

    @Test
    @DisplayName("@AuthenticationPrincipal이 붙은 JwtPrincipal 파라미터만 지원한다")
    void supportsParameter() throws NoSuchMethodException {
        MethodParameter annotated = annotatedParameter();
        MethodParameter notAnnotated = new MethodParameter(
                JwtPrincipalArgumentResolverTest.class.getDeclaredMethod("dummyControllerWithoutAnnotation", JwtPrincipal.class), 0);

        assertThat(resolver.supportsParameter(annotated)).isTrue();
        assertThat(resolver.supportsParameter(notAnnotated)).isFalse();
    }

    @Test
    @DisplayName("정상 access token이면 memberId가 담긴 JwtPrincipal을 반환한다")
    void resolveArgument_success() throws Exception {
        // given
        given(webRequest.getHeader("Authorization")).willReturn(AUTH_HEADER);
        given(jwtProvider.parseClaims(TOKEN)).willReturn(
                Jwts.claims().subject("1").add("purpose", "ACCESS").build());

        // when
        Object result = resolver.resolveArgument(annotatedParameter(), null, webRequest, null);

        // then
        assertThat(result).isEqualTo(new JwtPrincipal(1L));
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 예외가 발생한다")
    void resolveArgument_missingHeader() {
        // given
        given(webRequest.getHeader("Authorization")).willReturn(null);

        // when & then
        assertThatThrownBy(() -> resolver.resolveArgument(annotatedParameter(), null, webRequest, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(GlobalErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    @DisplayName("위변조된 토큰이면 예외가 발생한다")
    void resolveArgument_malformedToken() {
        // given
        given(webRequest.getHeader("Authorization")).willReturn(AUTH_HEADER);
        given(jwtProvider.parseClaims(TOKEN)).willThrow(new MalformedJwtException("malformed"));

        // when & then
        assertThatThrownBy(() -> resolver.resolveArgument(annotatedParameter(), null, webRequest, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(GlobalErrorCode.INVALID_TOKEN.getMessage());
    }

    @Test
    @DisplayName("purpose가 ACCESS가 아니면 예외가 발생한다")
    void resolveArgument_wrongPurpose() {
        // given
        given(webRequest.getHeader("Authorization")).willReturn(AUTH_HEADER);
        Claims refreshClaims = Jwts.claims().subject("1").add("purpose", "REFRESH").build();
        given(jwtProvider.parseClaims(TOKEN)).willReturn(refreshClaims);

        // when & then
        assertThatThrownBy(() -> resolver.resolveArgument(annotatedParameter(), null, webRequest, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(GlobalErrorCode.INVALID_TOKEN.getMessage());
    }
}