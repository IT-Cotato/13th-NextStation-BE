package com.cotato.nextstation.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticationPrincipal {

    /**
     * 비로그인도 허용하는 API에는 {@code false}를 준다. 토큰이 없으면 401 대신 {@code null}이 들어온다.
     * <p>
     * 둘러보기처럼 로그인 없이 볼 수 있으면서, 로그인했으면 좋아요 여부 같은 개인화 정보를
     * 채워줘야 하는 화면에 쓴다.
     * <p>
     * 토큰을 보냈는데 만료·위조인 경우는 이 값과 무관하게 401이다.
     */
    boolean required() default true;
}