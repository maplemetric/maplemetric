package com.maplemetric;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithStructureTest {

    @Test
    void 모듈의존관계를검증한다() {
        ApplicationModules.of(
                MaplemetricServiceApplication.class
        ).verify();
    }
}