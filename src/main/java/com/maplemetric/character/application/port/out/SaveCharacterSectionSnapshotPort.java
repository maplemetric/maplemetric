package com.maplemetric.character.application.port.out;

import java.time.Instant;
import java.util.Optional;

/**
 * 캐릭터 조회 결과를 구간별로 저장하고 읽는다.
 *
 * 같은 ocid·구간은 덮어쓴다. 이 저장본은 "마지막으로 가져온 결과"이며 이력이 아니다.
 * 일자별 이력이 필요해지면 별도 저장소를 두고 여기를 바꾸지 않는다.
 *
 * 구간은 화면 탭 경계와 같다. 탭을 열지 않은 구간은 저장본이 없을 수 있으므로
 * 호출자가 구간별로 존재 여부를 확인한다.
 */
public interface SaveCharacterSectionSnapshotPort {

    void save(
            String ocid,
            String characterName,
            CharacterSection section,
            String payload,
            Instant fetchedAt
    );

    Optional<CharacterSectionSnapshot> findByOcid(
            String ocid,
            CharacterSection section
    );

    Optional<CharacterSectionSnapshot> findByCharacterName(
            String characterName,
            CharacterSection section
    );

    record CharacterSectionSnapshot(
            String ocid,
            String characterName,
            CharacterSection section,
            String payload,
            Instant fetchedAt
    ) {
    }

    /**
     * 저장 구간이다.
     *
     * 화면 탭과 같은 경계를 쓴다. 프로토타입의 탭 구성이 스탯·장비 / 스킬·심볼 / AI
     * 분석이고, 장비는 갱신 빈도가 달라 따로 둔다.
     */
    enum CharacterSection {

        /** 기본 정보·스탯·랭킹·유니온·무릉·인기도·하이퍼스탯·어빌리티 */
        PROFILE,

        /** 장비와 세트 효과 */
        EQUIPMENT,

        /** 스킬·심볼·HEXA·V매트릭스·링크 스킬 */
        SKILL
    }
}
