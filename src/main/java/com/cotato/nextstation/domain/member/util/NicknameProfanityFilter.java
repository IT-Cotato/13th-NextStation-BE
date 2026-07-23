package com.cotato.nextstation.domain.member.util;

public interface NicknameProfanityFilter {

    boolean containsBannedWord(String nickname);
}