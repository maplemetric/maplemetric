package com.maplemetric.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maplemetric.ranking.infrastructure.persistence.JobAliasEntity;
import com.maplemetric.ranking.infrastructure.persistence.JobAliasRepository;
import com.maplemetric.ranking.infrastructure.persistence.JobAliasType;
import com.maplemetric.ranking.infrastructure.persistence.JobEntity;
import com.maplemetric.ranking.infrastructure.persistence.JobRepository;
import com.maplemetric.world.infrastructure.persistence.WorldAliasEntity;
import com.maplemetric.world.infrastructure.persistence.WorldAliasRepository;
import com.maplemetric.world.infrastructure.persistence.WorldAliasType;
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
class JobWorldAliasPersistenceTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobAliasRepository jobAliasRepository;

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private WorldAliasRepository worldAliasRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private int jobSlugSequence;

    private int worldSlugSequence;

    @Test
    void Job_Alias를저장하고Canonical직업으로조회한다() {
        JobEntity job = createJob("테스트직업1");

        JobAliasEntity saved = jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "TestJob1", JobAliasType.NEXON)
        );

        assertThat(saved.getId()).isNotNull();

        entityManager.clear();

        JobAliasEntity found = jobAliasRepository
                .findById(saved.getId())
                .orElseThrow();

        assertThat(found.getAliasName()).isEqualTo("TestJob1");
        assertThat(found.getAliasType()).isEqualTo(JobAliasType.NEXON);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getDeletedAt()).isNull();
        assertThat(found.getJob().getId()).isEqualTo(job.getId());
        assertThat(found.getJob().getJobName()).isEqualTo("테스트직업1");
    }

    @Test
    void 활성Job_Alias이름대소문자만다르면중복으로거부한다() {
        JobEntity job = createJob("테스트직업2");

        jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "hero", JobAliasType.NEXON)
        );

        assertThatThrownBy(() -> jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "HERO", JobAliasType.MANUAL)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 활성Job_Alias이름앞뒤공백만다르면중복으로거부한다() {
        JobEntity job = createJob("테스트직업3");

        jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "hero", JobAliasType.NEXON)
        );

        assertThatThrownBy(() -> jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, " hero ", JobAliasType.MANUAL)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은직업에PRIMARY_Alias를2개등록하면거부한다() {
        JobEntity job = createJob("테스트직업4");

        jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "hero-primary", JobAliasType.PRIMARY)
        );

        assertThatThrownBy(() -> jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "hero-primary-2", JobAliasType.PRIMARY)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은직업에NEXON과HISTORICAL은함께등록할수있다() {
        JobEntity job = createJob("테스트직업5");

        jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "hero-nexon", JobAliasType.NEXON)
        );

        JobAliasEntity historical = jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "hero-historical", JobAliasType.HISTORICAL)
        );

        assertThat(historical.getId()).isNotNull();
    }

    @Test
    void PRIMARY_Alias가0개인상태를정상허용한다() {
        JobEntity job = createJob("테스트직업6");

        jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "hero-nexon-only", JobAliasType.NEXON)
        );

        assertThat(
                jobAliasRepository.findAll().stream()
                        .filter(alias -> alias.getJob().getId().equals(job.getId()))
        ).hasSize(1);
    }

    @Test
    void SoftDelete된Job_Alias이름은동일이름재등록을막지않는다() {
        JobEntity job = createJob("테스트직업7");

        JobAliasEntity alias = jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "hero-reuse", JobAliasType.NEXON)
        );

        alias.softDelete(Instant.parse("2026-07-27T00:00:00Z"));
        jobAliasRepository.saveAndFlush(alias);

        JobAliasEntity reused = jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "hero-reuse", JobAliasType.MANUAL)
        );

        assertThat(reused.getId()).isNotNull();
    }

    @Test
    void 잘못된Job_Alias유형값은CHECK제약으로거부한다() {
        JobEntity job = createJob("테스트직업8");

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO p_job_alias (job_id, alias_name, alias_type)
                VALUES (?, ?, ?)
                """,
                job.getId(),
                "hero-invalid",
                "UNKNOWN"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 존재하지않는job_id참조는FK위반으로거부한다() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO p_job_alias (job_id, alias_name, alias_type)
                VALUES (gen_random_uuid(), ?, ?)
                """,
                "hero-orphan",
                "MANUAL"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void Alias가남아있는직업행삭제는FK_RESTRICT로거부한다() {
        JobEntity job = createJob("테스트직업9");

        jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "hero-restrict", JobAliasType.NEXON)
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM p_job WHERE job_id = ?",
                job.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void World_Alias를저장하고Canonical월드로조회한다() {
        WorldEntity world = createWorld("테스트월드1");

        WorldAliasEntity saved = worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(world, "TestWorld1", WorldAliasType.NEXON)
        );

        assertThat(saved.getId()).isNotNull();

        entityManager.clear();

        WorldAliasEntity found = worldAliasRepository
                .findById(saved.getId())
                .orElseThrow();

        assertThat(found.getAliasName()).isEqualTo("TestWorld1");
        assertThat(found.getAliasType()).isEqualTo(WorldAliasType.NEXON);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getDeletedAt()).isNull();
        assertThat(found.getWorld().getId()).isEqualTo(world.getId());
        assertThat(found.getWorld().getWorldName()).isEqualTo("테스트월드1");
    }

    @Test
    void 활성World_Alias이름대소문자만다르면중복으로거부한다() {
        WorldEntity world = createWorld("테스트월드2");

        worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(world, "luna", WorldAliasType.NEXON)
        );

        assertThatThrownBy(() -> worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(world, "LUNA", WorldAliasType.MANUAL)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은월드에PRIMARY_Alias를2개등록하면거부한다() {
        WorldEntity world = createWorld("테스트월드3");

        worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(world, "luna-primary", WorldAliasType.PRIMARY)
        );

        assertThatThrownBy(() -> worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(world, "luna-primary-2", WorldAliasType.PRIMARY)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 활성World_Alias이름앞뒤공백만다르면중복으로거부한다() {
        WorldEntity world = createWorld("테스트월드5");

        worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(world, "luna-trim", WorldAliasType.NEXON)
        );

        assertThatThrownBy(() -> worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(world, " luna-trim ", WorldAliasType.MANUAL)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 잘못된World_Alias유형값은CHECK제약으로거부한다() {
        WorldEntity world = createWorld("테스트월드6");

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO p_world_alias (world_id, alias_name, alias_type)
                VALUES (?, ?, ?)
                """,
                world.getId(),
                "luna-invalid",
                "UNKNOWN"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void SoftDelete된World_Alias이름은동일이름재등록을막지않는다() {
        WorldEntity world = createWorld("테스트월드7");

        WorldAliasEntity alias = worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(world, "luna-reuse", WorldAliasType.NEXON)
        );

        alias.softDelete(Instant.parse("2026-07-27T00:00:00Z"));
        worldAliasRepository.saveAndFlush(alias);

        WorldAliasEntity reused = worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(world, "luna-reuse", WorldAliasType.MANUAL)
        );

        assertThat(reused.getId()).isNotNull();
    }

    @Test
    void 존재하지않는world_id참조는FK위반으로거부한다() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO p_world_alias (world_id, alias_name, alias_type)
                VALUES (gen_random_uuid(), ?, ?)
                """,
                "luna-orphan",
                "MANUAL"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void Alias가남아있는월드행삭제는FK_RESTRICT로거부한다() {
        WorldEntity world = createWorld("테스트월드4");

        worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(world, "luna-restrict", WorldAliasType.NEXON)
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM p_world WHERE world_id = ?",
                world.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 정규화된이름으로활성Job_Alias를조회한다() {
        JobEntity job = createJob("테스트직업10");

        jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, " Hero-Lookup ", JobAliasType.NEXON)
        );

        entityManager.clear();

        assertThat(
                jobAliasRepository
                        .findActiveByNormalizedAliasName("hero-lookup")
        ).isPresent()
                .get()
                .extracting(alias -> alias.getJob().getJobName())
                .isEqualTo("테스트직업10");
    }

    @Test
    void SoftDelete된Job_Alias는정규화이름조회에서제외한다() {
        JobEntity job = createJob("테스트직업11");

        JobAliasEntity alias = jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "hero-deleted", JobAliasType.NEXON)
        );

        alias.softDelete(Instant.parse("2026-07-27T00:00:00Z"));
        jobAliasRepository.saveAndFlush(alias);

        entityManager.clear();

        assertThat(
                jobAliasRepository
                        .findActiveByNormalizedAliasName("hero-deleted")
        ).isEmpty();
    }

    @Test
    void 정규화된이름으로활성World_Alias를조회한다() {
        WorldEntity world = createWorld("테스트월드10");

        worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(world, " Luna-Lookup ", WorldAliasType.NEXON)
        );

        entityManager.clear();

        assertThat(
                worldAliasRepository
                        .findActiveByNormalizedAliasName("luna-lookup")
        ).isPresent()
                .get()
                .extracting(alias -> alias.getWorld().getWorldName())
                .isEqualTo("테스트월드10");
    }

    @Test
    void SoftDelete된World_Alias는정규화이름조회에서제외한다() {
        WorldEntity world = createWorld("테스트월드11");

        WorldAliasEntity alias = worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(world, "luna-deleted", WorldAliasType.NEXON)
        );

        alias.softDelete(Instant.parse("2026-07-27T00:00:00Z"));
        worldAliasRepository.saveAndFlush(alias);

        entityManager.clear();

        assertThat(
                worldAliasRepository
                        .findActiveByNormalizedAliasName("luna-deleted")
        ).isEmpty();
    }

    @Test
    void SoftDelete된Canonical직업의Alias는정규화이름조회에서제외한다() {
        JobEntity job = createJob("테스트직업12");

        jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "hero-deleted-job", JobAliasType.NEXON)
        );

        job.softDelete(Instant.parse("2026-07-27T00:00:00Z"));
        jobRepository.saveAndFlush(job);

        entityManager.clear();

        assertThat(
                jobAliasRepository
                        .findActiveByNormalizedAliasName("hero-deleted-job")
        ).isEmpty();
    }

    @Test
    void SoftDelete된Canonical월드의Alias는정규화이름조회에서제외한다() {
        WorldEntity world = createWorld("테스트월드12");

        worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(world, "luna-deleted-world", WorldAliasType.NEXON)
        );

        world.softDelete(Instant.parse("2026-07-27T00:00:00Z"));
        worldRepository.saveAndFlush(world);

        entityManager.clear();

        assertThat(
                worldAliasRepository
                        .findActiveByNormalizedAliasName("luna-deleted-world")
        ).isEmpty();
    }

    private JobEntity createJob(String jobName) {
        return jobRepository.saveAndFlush(
                JobEntity.create(
                        jobName,
                        "test-job-" + ++jobSlugSequence,
                        null,
                        "테스트계열",
                        "전사",
                        true,
                        0
                )
        );
    }

    private WorldEntity createWorld(String worldName) {
        return worldRepository.saveAndFlush(
                WorldEntity.create(
                        worldName,
                        "test-world-" + ++worldSlugSequence,
                        null,
                        0,
                        WorldEntity.Status.ACTIVE
                )
        );
    }
}
