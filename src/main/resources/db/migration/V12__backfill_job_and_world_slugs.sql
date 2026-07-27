DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM p_job WHERE job_slug IS NOT NULL)
        OR EXISTS (SELECT 1 FROM p_world WHERE world_slug IS NOT NULL) THEN
        RAISE EXCEPTION
            'p_job 또는 p_world에 기존 Slug가 있어 Backfill을 중단합니다.';
    END IF;
END $$;

UPDATE p_job AS job
SET job_slug = mapping.job_slug
FROM (
    VALUES
        ('다크나이트', 'dark-knight'),
        ('팔라딘', 'paladin'),
        ('히어로', 'hero'),
        ('비숍', 'bishop'),
        ('아크메이지(불,독)', 'arch-mage-fire-poison'),
        ('아크메이지(썬,콜)', 'arch-mage-ice-lightning'),
        ('보우마스터', 'bowmaster'),
        ('신궁', 'marksman'),
        ('패스파인더', 'pathfinder'),
        ('나이트로드', 'night-lord'),
        ('듀얼블레이더', 'dual-blader'),
        ('섀도어', 'shadower'),
        ('바이퍼', 'viper'),
        ('캐논슈터', 'cannon-shooter'),
        ('캡틴', 'captain'),
        ('미하일', 'mihile'),
        ('소울마스터', 'soul-master'),
        ('플레임위자드', 'flame-wizard'),
        ('윈드브레이커', 'wind-breaker'),
        ('나이트워커', 'night-walker'),
        ('스트라이커', 'striker'),
        ('블래스터', 'blaster'),
        ('배틀메이지', 'battle-mage'),
        ('와일드헌터', 'wild-hunter'),
        ('메카닉', 'mechanic'),
        ('제논', 'xenon'),
        ('데몬슬레이어', 'demon-slayer'),
        ('데몬어벤져', 'demon-avenger'),
        ('아란', 'aran'),
        ('루미너스', 'luminous'),
        ('에반', 'evan'),
        ('메르세데스', 'mercedes'),
        ('팬텀', 'phantom'),
        ('은월', 'eunwol'),
        ('카이저', 'kaiser'),
        ('카인', 'kain'),
        ('카데나', 'cadena'),
        ('엔젤릭버스터', 'angelic-buster'),
        ('아델', 'adele'),
        ('일리움', 'illium'),
        ('칼리', 'khali'),
        ('아크', 'ark'),
        ('렌', 'ren'),
        ('라라', 'lara'),
        ('호영', 'hoyoung'),
        ('제로', 'zero'),
        ('키네시스', 'kinesis'),
        ('레테', 'lethe')
) AS mapping(job_name, job_slug)
WHERE job.job_name = mapping.job_name;

UPDATE p_world AS world
SET world_slug = mapping.world_slug
FROM (
    VALUES
        ('오로라', 'aurora'),
        ('레드', 'red'),
        ('이노시스', 'enosis'),
        ('유니온', 'union'),
        ('스카니아', 'scania'),
        ('루나', 'luna'),
        ('제니스', 'zenith'),
        ('크로아', 'croa'),
        ('베라', 'bera'),
        ('엘리시움', 'elysium'),
        ('아케인', 'arcane'),
        ('노바', 'nova'),
        ('에오스', 'eos'),
        ('핼리오스', 'helios')
) AS mapping(world_name, world_slug)
WHERE world.world_name = mapping.world_name;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM p_job WHERE job_slug IS NULL)
        OR EXISTS (SELECT 1 FROM p_world WHERE world_slug IS NULL) THEN
        RAISE EXCEPTION
            'Slug가 Backfill되지 않은 Canonical 직업 또는 월드가 있습니다.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM p_job
        WHERE job_slug !~ '^[a-z0-9]+(-[a-z0-9]+)*$'
    ) OR EXISTS (
        SELECT 1
        FROM p_world
        WHERE world_slug !~ '^[a-z0-9]+(-[a-z0-9]+)*$'
    ) THEN
        RAISE EXCEPTION
            'lowercase kebab-case 형식이 아닌 Slug가 있습니다.';
    END IF;

    IF EXISTS (
        SELECT job_slug
        FROM p_job
        GROUP BY job_slug
        HAVING COUNT(*) > 1
    ) OR EXISTS (
        SELECT world_slug
        FROM p_world
        GROUP BY world_slug
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            '중복된 직업 또는 월드 Slug가 있습니다.';
    END IF;
END $$;
