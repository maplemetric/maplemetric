package com.maplemetric.character.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort.CharacterEquipment;
import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort.ItemEquipment;
import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort.ItemOption;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterEquipmentResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonCharacterEquipmentAdapterTest {

    private static final String OCID = "test-ocid";

    @Mock
    private CharacterClient characterClient;

    @Test
    void Nexon장비응답을Application장비정보로변환한다() {
        NexonCharacterEquipmentAdapter adapter =
                new NexonCharacterEquipmentAdapter(
                        characterClient
                );

        given(characterClient.getCharacterEquipment(OCID))
                .willReturn(createResponse());

        CharacterEquipment equipment =
                adapter.loadCharacterEquipment(OCID);

        assertThat(equipment.date())
                .isEqualTo("2026-07-23T00:00+09:00");
        assertThat(equipment.characterGender())
                .isEqualTo("남");
        assertThat(equipment.characterClass())
                .isEqualTo("팬텀");
        assertThat(equipment.presetNo())
                .isEqualTo(1);
        assertThat(equipment.itemEquipment())
                .containsExactly(createExpectedItem());
        assertThat(equipment.itemEquipmentPreset1())
                .containsExactly(createExpectedItem());
        assertThat(equipment.itemEquipmentPreset2())
                .isNull();
        assertThat(equipment.itemEquipmentPreset3())
                .isEmpty();

        verify(characterClient).getCharacterEquipment(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    private CharacterEquipmentResponse createResponse() {
        CharacterEquipmentResponse.ItemEquipment item =
                createResponseItem();

        return new CharacterEquipmentResponse(
                "2026-07-23T00:00+09:00",
                "남",
                "팬텀",
                1,
                List.of(item),
                List.of(item),
                null,
                List.of()
        );
    }

    private CharacterEquipmentResponse.ItemEquipment createResponseItem() {
        CharacterEquipmentResponse.ItemOption option =
                createResponseOption();

        return new CharacterEquipmentResponse.ItemEquipment(
                "무기",
                "무기",
                "테스트 무기",
                "https://example.com/item.png",
                "테스트 설명",
                "테스트 외형",
                "https://example.com/shape.png",
                "남",
                option,
                3,
                "100",
                2,
                "12",
                "5",
                "적용",
                "1",
                "2",
                "테스트 소울",
                "공격력 : +3%",
                option,
                option,
                null,
                "22",
                "미적용",
                "레전드리",
                "에픽",
                "보스 몬스터 공격 시 데미지 : +40%",
                "공격력 : +12%",
                "공격력 : +9%",
                "공격력 : +6%",
                "데미지 : +3%",
                "올스탯 : +3%",
                4,
                "2026-12-31T00:00+09:00"
        );
    }

    private CharacterEquipmentResponse.ItemOption createResponseOption() {
        return new CharacterEquipmentResponse.ItemOption(
                "10",
                "20",
                "30",
                "40",
                "50",
                "60",
                "70",
                "80",
                "90",
                "100",
                "110",
                "12",
                "13",
                "14",
                "15",
                16,
                "17",
                "18"
        );
    }

    private ItemEquipment createExpectedItem() {
        ItemOption option = createExpectedOption();

        return new ItemEquipment(
                "무기",
                "무기",
                "테스트 무기",
                "https://example.com/item.png",
                "테스트 설명",
                "테스트 외형",
                "https://example.com/shape.png",
                "남",
                option,
                3,
                "100",
                2,
                "12",
                "5",
                "적용",
                "1",
                "2",
                "테스트 소울",
                "공격력 : +3%",
                option,
                option,
                null,
                "22",
                "미적용",
                "레전드리",
                "에픽",
                "보스 몬스터 공격 시 데미지 : +40%",
                "공격력 : +12%",
                "공격력 : +9%",
                "공격력 : +6%",
                "데미지 : +3%",
                "올스탯 : +3%",
                4,
                "2026-12-31T00:00+09:00"
        );
    }

    private ItemOption createExpectedOption() {
        return new ItemOption(
                "10",
                "20",
                "30",
                "40",
                "50",
                "60",
                "70",
                "80",
                "90",
                "100",
                "110",
                "12",
                "13",
                "14",
                "15",
                16,
                "17",
                "18"
        );
    }
}
