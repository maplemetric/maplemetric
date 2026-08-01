package com.maplemetric.character.application.result;

/**
 * 저장 구간별로 묶은 조회 결과다.
 *
 * 화면 탭 경계와 같게 나눈다. 장비만 바뀌어 갱신하는 경우 스킬·심볼 구간을 다시
 * 가져오지 않아도 되게 하려는 것이다.
 *
 * 종합 응답은 세 구간을 합쳐 만든다. 구간 하나라도 없으면 합칠 수 없으므로 호출자가
 * 전체를 다시 수집한다.
 */
public final class CharacterSectionPayload {

    private CharacterSectionPayload() {
    }

    /** 기본 정보·스탯·랭킹·유니온·무릉·인기도·하이퍼스탯·어빌리티 */
    public record Profile(
            GetCharacterBasicResult basic,
            GetCharacterStatResult stat,
            GetCharacterRankingResult ranking,
            GetCharacterUnionResult union,
            GetCharacterPopularityResult popularity,
            GetCharacterHyperStatResult hyperStat,
            GetCharacterAbilityResult ability,
            GetCharacterDojangResult dojang
    ) {
    }

    /** 장비와 세트 효과 */
    public record Equipment(
            GetCharacterEquipmentResult equipment,
            GetCharacterSetEffectResult setEffect
    ) {
    }

    /** 스킬·심볼·HEXA·V매트릭스·링크 스킬 */
    public record Skill(
            GetCharacterSymbolResult symbols,
            GetCharacterSkillsResult skills,
            GetCharacterHexaResult hexa
    ) {
    }
}
