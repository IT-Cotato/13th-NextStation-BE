package com.cotato.nextstation.domain.member.util;

import com.vane.badwordfiltering.BadWordFiltering;
import org.springframework.stereotype.Component;

/**
 * io.github.vaneproject:badwordfiltering 라이브러리 기반 구현체
 **/
@Component
public class BadWordFilteringNicknameProfanityFilter implements NicknameProfanityFilter {

    private final BadWordFiltering badWordFiltering = new BadWordFiltering();

    @Override
    public boolean containsBannedWord(String nickname) {
        return badWordFiltering.check(nickname);
    }
}