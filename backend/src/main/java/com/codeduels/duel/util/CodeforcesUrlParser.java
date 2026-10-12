package com.codeduels.duel.util;


import jakarta.validation.ValidationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CodeforcesUrlParser {

    private static final Pattern CF_PATTERN = Pattern.compile(
            "codeforces\\.com/(?:contest|gym)/(\\d+)/problem/([A-Za-z]\\d?)"
                    + "|codeforces\\.com/problemset/problem/(\\d+)/([A-Za-z]\\d?)");

    public String parse(String url) {
        Matcher matcher = CF_PATTERN.matcher(url);
        if (!matcher.find()) {
            throw new ValidationException("Not a valid Codeforces problem URL: " + url);
        }
        String contestId = matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
        String index = matcher.group(2) != null ? matcher.group(2) : matcher.group(4);
        return contestId + index.toUpperCase();
    }

    public List<String> parseAll(List<String> urls) {
        return urls.stream().map(this::parse).toList();
    }
}