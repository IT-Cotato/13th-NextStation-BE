package com.cotato.nextstation.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 문자열 목록의 모든 값이 지정한 enum의 이름인지 검증한다.
 * <p>
 * 검증할 enum을 값으로 받으므로 특정 도메인에 묶이지 않는다.
 * <p>
 * 값이 null이거나 비어 있으면 통과시킨다. 필수 여부와 개수는 관심사가 달라
 * {@code @NotNull}·{@code @Size}로 따로 건다.
 */
@Documented
@Constraint(validatedBy = EnumNamesValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER,
        ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumNames {

    // 허용할 이름을 가진 enum 타입
    Class<? extends Enum<?>> value();

    // 같은 값을 두 번 넣는 것은 대부분 의미가 없어 기본은 중복 불가다.
    boolean allowDuplicates() default false;

    String message() default "허용되지 않는 값입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
