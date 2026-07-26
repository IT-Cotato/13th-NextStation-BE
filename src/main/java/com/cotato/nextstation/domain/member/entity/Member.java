package com.cotato.nextstation.domain.member.entity;

import com.cotato.nextstation.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Column(unique = true, length = 254) // RFC 5321 이메일 최대 길이
    private String email;

    @Column(length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Column(unique = true, length = 30)
    private String nickname;

    @Column(name = "profile_image_url", length = 1000) // presigned URL은 서명 쿼리스트링 때문에 길어질 수 있음
    private String profileImageUrl;

    @Column(name = "profile_bio", length = 30)
    private String profileBio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Member(String email, String password) {
        this.email = email;
        this.password = password;
        this.status = MemberStatus.PENDING;
        this.role = MemberRole.USER;
        this.gender = Gender.UNSPECIFIED;
    }

    public void completeProfile(String nickname, String profileImageUrl, Gender gender, LocalDate birthDate) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.gender = gender;
        this.birthDate = birthDate;
        this.status = MemberStatus.ACTIVE;
    }

    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        this.deletedAt = LocalDateTime.now();
    }
}