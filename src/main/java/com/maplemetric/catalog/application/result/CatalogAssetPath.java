package com.maplemetric.catalog.application.result;

/**
 * Canonical Slug에서 정적 Asset 경로를 유도한다.
 *
 * Asset 경로는 Slug와 1:1로 정해지는 규칙이므로 DB 컬럼을 두지 않는다.
 * 파일이 실제로 존재하는지는 Backend가 검증하지 않으며, Asset 소유권과 Versioning은
 * API 명세 §5.3의 별도 범위다.
 */
final class CatalogAssetPath {

    private static final String JOB_ICON_FORMAT = "/assets/jobs/%s/icon.webp";

    private static final String WORLD_LOGO_FORMAT =
            "/assets/worlds/%s/logo.webp";

    private CatalogAssetPath() {
    }

    static String jobIconUrl(String jobSlug) {
        return format(JOB_ICON_FORMAT, jobSlug);
    }

    static String worldLogoUrl(String worldSlug) {
        return format(WORLD_LOGO_FORMAT, worldSlug);
    }

    private static String format(String pathFormat, String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }

        return String.format(pathFormat, slug);
    }
}
