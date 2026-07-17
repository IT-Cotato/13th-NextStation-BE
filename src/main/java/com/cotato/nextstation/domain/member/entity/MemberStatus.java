package com.cotato.nextstation.domain.member.entity;

public enum MemberStatus {
    PENDING, // 계정 생성됨, 프로필 설정 미완료
    ACTIVE, // 정상 이용
    SUSPENDED, // 정지된 계정
    WITHDRAWN // 탈퇴한 계정
}