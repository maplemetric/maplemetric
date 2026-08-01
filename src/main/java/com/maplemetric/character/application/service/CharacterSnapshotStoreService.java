package com.maplemetric.character.application.service;

import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort;
import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort.CharacterSection;
import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort.CharacterSectionSnapshot;
import com.maplemetric.character.application.result.CharacterSectionPayload;
import com.maplemetric.character.application.result.GetCharacterSummaryResult;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 캐릭터 종합 조회 결과를 구간별로 저장하고 다시 합친다.
 *
 * 종합 조회 한 번이 Nexon을 21회 호출한다. 저장본이 있으면 그 21회를 아낀다.
 *
 * 구간 하나라도 없으면 합치지 못하므로 없는 것으로 본다. 지금은 세 구간을 항상 함께
 * 저장하지만, 탭별 지연 로딩이 들어오면 구간이 따로 채워질 수 있다.
 */
@Slf4j
@Service
public class CharacterSnapshotStoreService {

    private final SaveCharacterSectionSnapshotPort snapshotPort;

    public CharacterSnapshotStoreService(
            SaveCharacterSectionSnapshotPort snapshotPort
    ) {
        this.snapshotPort = snapshotPort;
    }

    /**
     * 캐릭터명으로 저장본을 찾는다.
     *
     * ocid를 모른 채 찾을 수 있어야 ocid 조회 1회까지 아낄 수 있다.
     */
    @Transactional(readOnly = true)
    public Optional<StoredSummary> findByCharacterName(String characterName) {
        Optional<CharacterSectionSnapshot<CharacterSectionPayload.Profile>>
                profile = snapshotPort.findByCharacterName(
                        characterName,
                        CharacterSection.PROFILE,
                        CharacterSectionPayload.Profile.class
                );

        if (profile.isEmpty()) {
            return Optional.empty();
        }

        String ocid = profile.get().ocid();

        Optional<CharacterSectionSnapshot<CharacterSectionPayload.Equipment>>
                equipment = snapshotPort.findByOcid(
                        ocid,
                        CharacterSection.EQUIPMENT,
                        CharacterSectionPayload.Equipment.class
                );

        Optional<CharacterSectionSnapshot<CharacterSectionPayload.Skill>>
                skill = snapshotPort.findByOcid(
                        ocid,
                        CharacterSection.SKILL,
                        CharacterSectionPayload.Skill.class
                );

        if (equipment.isEmpty() || skill.isEmpty()) {
            return Optional.empty();
        }

        // 구간별 조회 시각이 다를 수 있다. 이 저장본 전체가 언제 것인지는 가장 오래된
        // 구간이 답이다. 지금은 세 구간을 함께 저장하지만 탭별 지연 로딩이 들어오면
        // 달라진다.
        Instant fetchedAt = oldest(
                profile.get().fetchedAt(),
                equipment.get().fetchedAt(),
                skill.get().fetchedAt()
        );

        return Optional.of(new StoredSummary(
                ocid,
                fetchedAt,
                assemble(
                        profile.get().payload(),
                        equipment.get().payload(),
                        skill.get().payload(),
                        fetchedAt
                )
        ));
    }

    @Transactional
    public void store(
            String ocid,
            GetCharacterSummaryResult summary,
            Instant fetchedAt
    ) {
        String characterName = summary.basic().characterName();

        snapshotPort.save(
                ocid,
                characterName,
                CharacterSection.PROFILE,
                new CharacterSectionPayload.Profile(
                        summary.basic(),
                        summary.stat(),
                        summary.ranking(),
                        summary.union(),
                        summary.popularity(),
                        summary.hyperStat(),
                        summary.ability(),
                        summary.dojang()
                ),
                fetchedAt
        );

        snapshotPort.save(
                ocid,
                characterName,
                CharacterSection.EQUIPMENT,
                new CharacterSectionPayload.Equipment(
                        summary.equipment(),
                        summary.setEffect()
                ),
                fetchedAt
        );

        snapshotPort.save(
                ocid,
                characterName,
                CharacterSection.SKILL,
                new CharacterSectionPayload.Skill(
                        summary.symbols(),
                        summary.skills(),
                        summary.hexa()
                ),
                fetchedAt
        );
    }

    private Instant oldest(
            Instant first,
            Instant second,
            Instant third
    ) {
        Instant oldest = first.isBefore(second) ? first : second;

        return oldest.isBefore(third) ? oldest : third;
    }

    private GetCharacterSummaryResult assemble(
            CharacterSectionPayload.Profile profile,
            CharacterSectionPayload.Equipment equipment,
            CharacterSectionPayload.Skill skill,
            Instant fetchedAt
    ) {
        return GetCharacterSummaryResult.of(
                profile.basic(),
                profile.stat(),
                profile.ranking(),
                profile.union(),
                skill.symbols(),
                skill.skills(),
                skill.hexa(),
                equipment.equipment(),
                equipment.setEffect(),
                profile.popularity(),
                profile.hyperStat(),
                profile.ability(),
                profile.dojang(),
                fetchedAt.toString()
        );
    }

    public record StoredSummary(
            String ocid,
            Instant fetchedAt,
            GetCharacterSummaryResult summary
    ) {
    }
}
