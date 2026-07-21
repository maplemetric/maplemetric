package com.maplemetric.character.application.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CharacterStatProfileTest {

    @Test
    void 특수직업은계산불가이며공격력유형이없다() {
        assertThat(CharacterStatProfile.XENON.calculable())
                .isFalse();
        assertThat(CharacterStatProfile.XENON.powerType())
                .isEqualTo(PowerType.NONE);
        assertThat(CharacterStatProfile.DEMON_AVENGER.calculable())
                .isFalse();
        assertThat(CharacterStatProfile.DEMON_AVENGER.powerType())
                .isEqualTo(PowerType.NONE);
    }

    @ParameterizedTest
    @MethodSource("verifiedCharacterClasses")
    void 공식응답직업명을프로필에정확히매핑한다(
            String characterClass,
            CharacterStatProfile expectedProfile
    ) {
        assertThat(CharacterStatProfile.from(characterClass))
                .contains(expectedProfile);
    }

    @ParameterizedTest
    @MethodSource("unverifiedCharacterClasses")
    void 정확히일치하지않는직업명은매핑하지않는다(
            String characterClass
    ) {
        assertThat(CharacterStatProfile.from(characterClass))
                .isEmpty();
    }

    private static Stream<Arguments> verifiedCharacterClasses() {
        return Stream.of(
                Arguments.of("히어로", CharacterStatProfile.STR),
                Arguments.of("팔라딘", CharacterStatProfile.STR),
                Arguments.of("다크나이트", CharacterStatProfile.STR),
                Arguments.of("바이퍼", CharacterStatProfile.STR),
                Arguments.of("캐논마스터", CharacterStatProfile.STR),
                Arguments.of("소울마스터", CharacterStatProfile.STR),
                Arguments.of("스트라이커", CharacterStatProfile.STR),
                Arguments.of("미하일", CharacterStatProfile.STR),
                Arguments.of("아란", CharacterStatProfile.STR),
                Arguments.of("데몬슬레이어", CharacterStatProfile.STR),
                Arguments.of("블래스터", CharacterStatProfile.STR),
                Arguments.of("카이저", CharacterStatProfile.STR),
                Arguments.of("제로", CharacterStatProfile.STR),
                Arguments.of("은월", CharacterStatProfile.STR),
                Arguments.of("아크", CharacterStatProfile.STR),
                Arguments.of("아델", CharacterStatProfile.STR),
                Arguments.of("렌", CharacterStatProfile.STR),
                Arguments.of("보우마스터", CharacterStatProfile.DEX),
                Arguments.of("신궁", CharacterStatProfile.DEX),
                Arguments.of("패스파인더", CharacterStatProfile.DEX),
                Arguments.of("캡틴", CharacterStatProfile.DEX),
                Arguments.of("윈드브레이커", CharacterStatProfile.DEX),
                Arguments.of("와일드헌터", CharacterStatProfile.DEX),
                Arguments.of("메카닉", CharacterStatProfile.DEX),
                Arguments.of("메르세데스", CharacterStatProfile.DEX),
                Arguments.of("엔젤릭버스터", CharacterStatProfile.DEX),
                Arguments.of("카인", CharacterStatProfile.DEX),
                Arguments.of("아크메이지(불,독)", CharacterStatProfile.INT),
                Arguments.of("아크메이지(썬,콜)", CharacterStatProfile.INT),
                Arguments.of("비숍", CharacterStatProfile.INT),
                Arguments.of("플레임위자드", CharacterStatProfile.INT),
                Arguments.of("에반", CharacterStatProfile.INT),
                Arguments.of("배틀메이지", CharacterStatProfile.INT),
                Arguments.of("루미너스", CharacterStatProfile.INT),
                Arguments.of("키네시스", CharacterStatProfile.INT),
                Arguments.of("일리움", CharacterStatProfile.INT),
                Arguments.of("라라", CharacterStatProfile.INT),
                Arguments.of("레테", CharacterStatProfile.INT),
                Arguments.of("나이트로드", CharacterStatProfile.LUK),
                Arguments.of("섀도어", CharacterStatProfile.LUK),
                Arguments.of("듀얼블레이더", CharacterStatProfile.LUK),
                Arguments.of("나이트워커", CharacterStatProfile.LUK),
                Arguments.of("팬텀", CharacterStatProfile.LUK),
                Arguments.of("카데나", CharacterStatProfile.LUK),
                Arguments.of("호영", CharacterStatProfile.LUK),
                Arguments.of("칼리", CharacterStatProfile.LUK),
                Arguments.of("제논", CharacterStatProfile.XENON),
                Arguments.of(
                        "데몬어벤져",
                        CharacterStatProfile.DEMON_AVENGER
                )
        );
    }

    private static Stream<String> unverifiedCharacterClasses() {
        return Stream.of(
                " 팬텀",
                "팬텀 ",
                "팬텀-전체 전직",
                "아크메이지",
                "초보자"
        );
    }
}
