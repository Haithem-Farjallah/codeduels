package com.codeduels.problem.dto;

import com.codeduels.problem.model.TestCase;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseDto {
    @NotBlank
    private String input;
    @NotBlank
    private String expectedOutput;
    @NotBlank
    private String explanation;


    public static List<TestCase> toTestCase(List<TestCaseDto> dtos) {
        return dtos.stream().map(d->new TestCase(d.getInput(), d.getExpectedOutput(), d.getExplanation())).collect(Collectors.toList());
    }

}
