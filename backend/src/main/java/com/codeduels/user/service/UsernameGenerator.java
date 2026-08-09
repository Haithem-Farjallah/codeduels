package com.codeduels.user.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class UsernameGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "swift", "clever", "brave", "silent", "cosmic", "turbo", "sneaky", "rapid",
            "crimson", "shadow", "golden", "frozen", "electric", "mighty", "quantum", "lunar",
            "savage", "epic", "wild", "noble", "fierce", "atomic", "hyper", "mystic");

    private static final List<String> NOUNS = List.of(
            "coder", "panda", "ninja", "raptor", "wizard", "falcon", "hacker", "byte",
            "otter", "phoenix", "tiger", "kraken", "viper", "dragon", "wolf", "cobra",
            "sparrow", "yeti", "golem", "comet", "goblin", "mantis", "lynx", "badger");

    private final Random random = new Random();

    public String generate() {
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(random.nextInt(NOUNS.size()));
        int number = random.nextInt(10_000);
        return adjective + "_" + noun + "_" + number;
    }
}
