package com.codeduels.problem.model;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {

    @Column(nullable = false)
    private String input;

    @Column(nullable = false)
    private String expectedOutput;

    @Column(nullable = false)
    private String explanation;
}
