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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {
    // 탈퇴 후 이 기간 안에는 로그인만으로 계정을 복구할 수 있고, 지나면 배치가 회원 행과 콘텐츠를 통째로 삭제한다.
    // 삭제 전까지는 email UNIQUE가 살아있어 같은 이메일로 재가입할 수 없다(그 기간엔 재가입 대신 복구를 제공).
    public static final Duration WITHDRAWAL_GRACE_PERIOD = Duration.ofDays(7);

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

    /**
     * 탈퇴 - soft delete. 개인정보는 그대로 두고 상태만 바꾼다.
     * 유예 기간 안에 {@link #restore()}로 되살릴 수 있어야 하므로 이 시점에는 아무것도 비우지 않는다.
     */
    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 유예 기간 내 복구.
     * 프로필 설정을 마치기 전에 탈퇴한 회원(nickname 없음)은 ACTIVE가 아니라 PENDING으로 되돌린다.
     */
    public void restore() {
        this.status = nickname == null ? MemberStatus.PENDING : MemberStatus.ACTIVE;
        this.deletedAt = null;
    }

    public boolean isWithdrawn() {
        return status == MemberStatus.WITHDRAWN;
    }

    /**
     * 유예 기간이 남아 복구할 수 있는 상태인지 여부
     */
    public boolean isRestorable() {
        return isWithdrawn()
                && deletedAt != null
                && deletedAt.isAfter(LocalDateTime.now().minus(WITHDRAWAL_GRACE_PERIOD));
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}