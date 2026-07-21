package com.maplemetric.character.application.calculator;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public enum CharacterStatProfile {

    STR(
            List.of(Stat.STR),
            List.of(Stat.DEX),
            PowerType.ATTACK_POWER,
            true
    ),
    DEX(
            List.of(Stat.DEX),
            List.of(Stat.STR),
            PowerType.ATTACK_POWER,
            true
    ),
    INT(
            List.of(Stat.INT),
            List.of(Stat.LUK),
            PowerType.MAGIC_POWER,
            true
    ),
    LUK(
            List.of(Stat.LUK),
            List.of(Stat.DEX),
            PowerType.ATTACK_POWER,
            true
    ),
    XENON(
            List.of(Stat.STR, Stat.DEX, Stat.LUK),
            List.of(),
            PowerType.NONE,
            false
    ),
    DEMON_AVENGER(
            List.of(Stat.MAX_HP),
            List.of(),
            PowerType.NONE,
            false
    );

    private static final Map<String, CharacterStatProfile> PROFILES =
            Map.ofEntries(
                    Map.entry("히어로", STR),
                    Map.entry("팔라딘", STR),
                    Map.entry("다크나이트", STR),
                    Map.entry("바이퍼", STR),
                    Map.entry("캐논마스터", STR),
                    Map.entry("소울마스터", STR),
                    Map.entry("스트라이커", STR),
                    Map.entry("미하일", STR),
                    Map.entry("아란", STR),
                    Map.entry("데몬슬레이어", STR),
                    Map.entry("블래스터", STR),
                    Map.entry("카이저", STR),
                    Map.entry("제로", STR),
                    Map.entry("은월", STR),
                    Map.entry("아크", STR),
                    Map.entry("아델", STR),
                    Map.entry("렌", STR),
                    Map.entry("보우마스터", DEX),
                    Map.entry("신궁", DEX),
                    Map.entry("패스파인더", DEX),
                    Map.entry("캡틴", DEX),
                    Map.entry("윈드브레이커", DEX),
                    Map.entry("와일드헌터", DEX),
                    Map.entry("메카닉", DEX),
                    Map.entry("메르세데스", DEX),
                    Map.entry("엔젤릭버스터", DEX),
                    Map.entry("카인", DEX),
                    Map.entry("아크메이지(불,독)", INT),
                    Map.entry("아크메이지(썬,콜)", INT),
                    Map.entry("비숍", INT),
                    Map.entry("플레임위자드", INT),
                    Map.entry("에반", INT),
                    Map.entry("배틀메이지", INT),
                    Map.entry("루미너스", INT),
                    Map.entry("키네시스", INT),
                    Map.entry("일리움", INT),
                    Map.entry("라라", INT),
                    Map.entry("레테", INT),
                    Map.entry("나이트로드", LUK),
                    Map.entry("섀도어", LUK),
                    Map.entry("듀얼블레이더", LUK),
                    Map.entry("나이트워커", LUK),
                    Map.entry("팬텀", LUK),
                    Map.entry("카데나", LUK),
                    Map.entry("호영", LUK),
                    Map.entry("칼리", LUK),
                    Map.entry("제논", XENON),
                    Map.entry("데몬어벤져", DEMON_AVENGER)
            );

    private final List<Stat> mainStats;
    private final List<Stat> subStats;
    private final PowerType powerType;
    private final boolean calculable;

    CharacterStatProfile(
            List<Stat> mainStats,
            List<Stat> subStats,
            PowerType powerType,
            boolean calculable
    ) {
        this.mainStats = mainStats;
        this.subStats = subStats;
        this.powerType = powerType;
        this.calculable = calculable;
    }

    public static Optional<CharacterStatProfile> from(
            String characterClass
    ) {
        return Optional.ofNullable(
                PROFILES.get(characterClass)
        );
    }

    public List<Stat> mainStats() {
        return mainStats;
    }

    public List<Stat> subStats() {
        return subStats;
    }

    public PowerType powerType() {
        return powerType;
    }

    public boolean calculable() {
        return calculable;
    }

    public enum Stat {
        STR,
        DEX,
        INT,
        LUK,
        MAX_HP
    }
}
