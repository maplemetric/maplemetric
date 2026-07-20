package com.maplemetric;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "maplemetric.nexon.open-api.key=test-api-key"
        }
)
class MaplemetricServiceApplicationTests {

    @Test
    void 애플리케이션컨텍스트를로드한다() {
    }
}