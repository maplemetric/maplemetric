package com.maplemetric;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithStructureTest {

    private final ApplicationModules modules =
            ApplicationModules.of(
                    MaplemetricServiceApplication.class
            );

    @Test
    void 모듈의존관계를검증한다() {
        modules.verify();
    }

    @Test
    void 공통Nexon인터페이스를공개한다() {
        assertThat(
                modules.getModuleByName("common")
                        .orElseThrow()
                        .getNamedInterfaces()
                        .getByName("nexon")
        ).isPresent();
    }

    @Test
    void 기존Global모듈은존재하지않는다() {
        assertThat(
                modules.getModuleByName("global")
        ).isEmpty();
    }
}