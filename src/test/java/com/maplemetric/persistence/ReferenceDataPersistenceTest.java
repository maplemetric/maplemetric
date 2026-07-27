package com.maplemetric.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maplemetric.ranking.infrastructure.persistence.JobEntity;
import com.maplemetric.ranking.infrastructure.persistence.JobRepository;
import com.maplemetric.world.infrastructure.persistence.WorldEntity;
import com.maplemetric.world.infrastructure.persistence.WorldRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true"
        }
)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class ReferenceDataPersistenceTest {

    private static final String POSTGRES_IMAGE =
            "postgres:17-alpine";

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 월드기준정보를저장하고이름으로조회한다() {
        WorldEntity world = WorldEntity.create(
                "테스트루나",
                "luna",
                1,
                WorldEntity.Status.ACTIVE
        );

        WorldEntity savedWorld =
                worldRepository.saveAndFlush(world);

        assertThat(savedWorld.getId()).isNotNull();
        assertThat(savedWorld.getCreatedAt()).isNotNull();
        assertThat(savedWorld.getUpdatedAt()).isNotNull();

        entityManager.clear();

        WorldEntity foundWorld = worldRepository
                .findByWorldNameAndDeletedAtIsNull("테스트루나")
                .orElseThrow();

        assertThat(foundWorld.getId())
                .isEqualTo(savedWorld.getId());
        assertThat(foundWorld.getExternalWorldCode())
                .isEqualTo("luna");
        assertThat(foundWorld.getStatus())
                .isEqualTo(WorldEntity.Status.ACTIVE);
    }

    @Test
    void 삭제된월드는이름조회에서제외한다() {
        WorldEntity world = worldRepository.saveAndFlush(
                WorldEntity.create(
                        "테스트스카니아",
                        "scania",
                        2,
                        WorldEntity.Status.ACTIVE
                )
        );

        Instant deletedAt =
                Instant.parse("2026-07-23T10:00:00Z");

        world.softDelete(deletedAt);
        worldRepository.saveAndFlush(world);

        entityManager.clear();

        assertThat(
                worldRepository
                        .findByWorldNameAndDeletedAtIsNull("테스트스카니아")
        ).isEmpty();
        assertThat(
                worldRepository.findById(world.getId())
        ).get()
                .extracting(WorldEntity::getDeletedAt)
                .isEqualTo(deletedAt);
    }

    @Test
    void 직업기준정보를저장하고이름으로조회한다() {
        JobEntity job = JobEntity.create(
                "테스트팬텀",
                "phantom",
                "영웅",
                "도적",
                true,
                1
        );

        JobEntity savedJob = jobRepository.saveAndFlush(job);

        assertThat(savedJob.getId()).isNotNull();
        assertThat(savedJob.getCreatedAt()).isNotNull();
        assertThat(savedJob.getUpdatedAt()).isNotNull();
        assertThat(savedJob.isAvailable()).isTrue();

        entityManager.clear();

        JobEntity foundJob = jobRepository
                .findByJobNameAndDeletedAtIsNull("테스트팬텀")
                .orElseThrow();

        assertThat(foundJob.getId()).isEqualTo(savedJob.getId());
        assertThat(foundJob.getExternalJobCode())
                .isEqualTo("phantom");
        assertThat(foundJob.getJobGroup()).isEqualTo("영웅");
        assertThat(foundJob.getJobBranch()).isEqualTo("도적");
    }

    @Test
    void 삭제된직업은이름조회에서제외한다() {
        JobEntity job = jobRepository.saveAndFlush(
                JobEntity.create(
                        "테스트히어로",
                        "hero",
                        "모험가",
                        "전사",
                        true,
                        2
                )
        );

        Instant deletedAt =
                Instant.parse("2026-07-23T11:00:00Z");

        job.softDelete(deletedAt);
        jobRepository.saveAndFlush(job);

        entityManager.clear();

        assertThat(
                jobRepository
                        .findByJobNameAndDeletedAtIsNull("테스트히어로")
        ).isEmpty();
        assertThat(jobRepository.findById(job.getId()))
                .get()
                .extracting(JobEntity::getDeletedAt)
                .isEqualTo(deletedAt);
    }

    @Test
    void 월드명중복을허용하지않는다() {
        worldRepository.saveAndFlush(
                WorldEntity.create(
                        "테스트루나",
                        "luna",
                        1,
                        WorldEntity.Status.ACTIVE
                )
        );

        WorldEntity duplicateWorld = WorldEntity.create(
                "테스트루나",
                "luna-duplicate",
                2,
                WorldEntity.Status.ACTIVE
        );

        assertThatThrownBy(
                () -> worldRepository.saveAndFlush(duplicateWorld)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 외부월드코드중복을허용하지않는다() {
        worldRepository.saveAndFlush(
                WorldEntity.create(
                        "테스트루나",
                        "luna",
                        1,
                        WorldEntity.Status.ACTIVE
                )
        );

        WorldEntity duplicateWorld = WorldEntity.create(
                "테스트루나2",
                "luna",
                2,
                WorldEntity.Status.ACTIVE
        );

        assertThatThrownBy(
                () -> worldRepository.saveAndFlush(duplicateWorld)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 직업명중복을허용하지않는다() {
        jobRepository.saveAndFlush(
                JobEntity.create(
                        "테스트팬텀",
                        "phantom",
                        "영웅",
                        "도적",
                        true,
                        1
                )
        );

        JobEntity duplicateJob = JobEntity.create(
                "테스트팬텀",
                "phantom-duplicate",
                "영웅",
                "도적",
                true,
                2
        );

        assertThatThrownBy(
                () -> jobRepository.saveAndFlush(duplicateJob)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 외부직업코드중복을허용하지않는다() {
        jobRepository.saveAndFlush(
                JobEntity.create(
                        "테스트팬텀",
                        "phantom",
                        "영웅",
                        "도적",
                        true,
                        1
                )
        );

        JobEntity duplicateJob = JobEntity.create(
                "테스트팬텀2",
                "phantom",
                "영웅",
                "도적",
                true,
                2
        );

        assertThatThrownBy(
                () -> jobRepository.saveAndFlush(duplicateJob)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 잘못된월드상태를허용하지않는다() {
        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO p_world (
                            world_name,
                            display_order,
                            status
                        ) VALUES (?, ?, ?)
                        """,
                        "테스트월드",
                        0,
                        "UNKNOWN"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 음수월드표시순서를허용하지않는다() {
        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO p_world (
                            world_name,
                            display_order,
                            status
                        ) VALUES (?, ?, ?)
                        """,
                        "테스트월드",
                        -1,
                        "ACTIVE"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 음수직업표시순서를허용하지않는다() {
        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO p_job (
                            job_name,
                            job_group,
                            display_order
                        ) VALUES (?, ?, ?)
                        """,
                        "테스트직업",
                        "테스트직업군",
                        -1
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
