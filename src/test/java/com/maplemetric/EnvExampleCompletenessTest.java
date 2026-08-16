package com.maplemetric;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 설정 파일이 읽는 환경변수가 예시 파일에 모두 적혀 있는지 확인한다.
 *
 * 예시 파일은 운영자가 무엇을 조정할 수 있는지 아는 유일한 통로다. 설정에 변수를
 * 추가하면서 예시를 잊으면, 그 변수는 코드를 읽는 사람만 알 수 있게 된다.
 *
 * 실제로 이 검사를 넣기 전에 열세 개가 빠져 있었다.
 */
class EnvExampleCompletenessTest {

    private static final Path ENV_EXAMPLE = Path.of(".env.example");

    private static final Path RESOURCES =
            Path.of("src", "main", "resources");

    /** {@code ${VAR}} 또는 {@code ${VAR:기본값}}에서 이름만 꺼낸다. */
    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\$\\{([A-Z][A-Z0-9_]*)[:}]");

    @Test
    void 설정이읽는환경변수가예시파일에모두있다() throws IOException {
        Set<String> declared = declaredInExample();

        assertThat(usedInConfiguration())
                .as("설정이 읽지만 .env.example에 없는 환경변수")
                .allSatisfy(variable ->
                        assertThat(declared).contains(variable));
    }

    /**
     * 예시 파일에만 있고 아무도 읽지 않는 변수는 없어야 한다.
     *
     * 읽히지 않는 변수를 적어 두면 값을 바꿔도 아무 일이 일어나지 않는다. 이름을
     * 바꾸고 예시를 고치지 않은 흔적이기도 하다.
     */
    @Test
    void 예시파일에읽히지않는환경변수가없다() throws IOException {
        Set<String> used = usedInConfiguration();

        assertThat(declaredInExample())
                .as("설정이 읽지 않는데 .env.example에 있는 환경변수")
                .allSatisfy(variable -> assertThat(used).contains(variable));
    }

    private Set<String> usedInConfiguration() throws IOException {
        Set<String> variables = new LinkedHashSet<>();

        try (Stream<Path> files = Files.list(RESOURCES)) {
            for (Path file : files
                    .filter(path -> path.getFileName().toString()
                            .endsWith(".yaml"))
                    .toList()) {
                Matcher matcher = PLACEHOLDER.matcher(read(file));

                while (matcher.find()) {
                    variables.add(matcher.group(1));
                }
            }
        }

        assertThat(variables)
                .as("설정 파일에서 환경변수를 하나도 찾지 못했다")
                .isNotEmpty();

        return variables;
    }

    private Set<String> declaredInExample() throws IOException {
        Set<String> variables = new LinkedHashSet<>();

        for (String line : read(ENV_EXAMPLE).split("\\R")) {
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            int separator = trimmed.indexOf('=');

            if (separator > 0) {
                variables.add(trimmed.substring(0, separator).trim());
            }
        }

        return variables;
    }

    private String read(Path path) throws IOException {
        assertThat(Files.exists(path))
                .as("%s", path)
                .isTrue();

        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
