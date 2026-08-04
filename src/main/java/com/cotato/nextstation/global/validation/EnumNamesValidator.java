package com.cotato.nextstation.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumNamesValidator implements ConstraintValidator<EnumNames, List<String>> {

    private static final String DUPLICATE_MESSAGE = "중복된 값은 넣을 수 없습니다.";

    private Set<String> allowedNames;
    private boolean allowDuplicates;

    @Override
    public void initialize(EnumNames annotation) {
        // 허용 이름은 요청마다 변하지 않으므로 한 번만 만들어 둔다.
        // 목록에 null이 섞여 와도 조회만으로 터지지 않도록 null 조회를 허용하는 HashSet을 쓴다.
        this.allowedNames = Arrays.stream(annotation.value().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toCollection(HashSet::new));
        this.allowDuplicates = annotation.allowDuplicates();
    }

    @Override
    public boolean isValid(List<String> values, ConstraintValidatorContext context) {
        // 값 자체가 없는 경우는 이 애노테이션의 관심사가 아니다. @NotNull·@Size가 판단한다.
        if (values == null || values.isEmpty()) {
            return true;
        }

        // 중복은 애노테이션의 message와 위반 사유가 달라 메시지를 따로 담는다.
        // 기본 메시지를 끄지 않으면 두 메시지가 함께 나간다.
        if (!allowDuplicates && new HashSet<>(values).size() != values.size()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(DUPLICATE_MESSAGE).addConstraintViolation();
            return false;
        }

        // 이름이 아닌 값이 섞여 있으면 애노테이션에 지정한 message가 그대로 나간다.
        return values.stream().allMatch(allowedNames::contains);
    }
}
