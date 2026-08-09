package com.codeduels.submission.model;

import java.util.Arrays;

public enum Language {
    PYTHON("python", 71),
    JAVA("java", 62),
    CPP("cpp", 54),
    JAVASCRIPT("javascript", 63);

    private final String slug;
    private final int judge0Id;

    Language(String slug, int judge0Id) {
        this.slug = slug;
        this.judge0Id = judge0Id;
    }
    public String getSlug() { return slug; }
    public int getJudge0Id() { return judge0Id; }

    public static Language fromSlug(String slug) {
        return Arrays.stream(values())
                .filter(l -> l.slug.equalsIgnoreCase(slug))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported language: " + slug));
    }
}
