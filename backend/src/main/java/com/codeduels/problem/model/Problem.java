package com.codeduels.problem.model;

import com.codeduels.common.entity.UuidBaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "problem")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Problem extends UuidBaseEntity {

    @Enumerated(EnumType.STRING)
    private ProblemStatus status;

    @Column(nullable = false,unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition  = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT",name = "constraints_text")
    private String constraints;

    @Column(nullable = false)
    private int points;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Column(nullable = false, name = "time_limit_ms")
    private int timeLimitMs;

    @Column(nullable = false,name = "memory_limit_kb")
    private int memoryLimitKb;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition="jsonb",name = "sample_test_cases")
    private List<TestCase> sampleTestCases=new ArrayList<>();

    @Column(name = "hidden_test_cases_s3_key")
    private String hiddenTestCasesS3Key;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"),
            name="problem_tag"
    )
    private Set<Tag> tags=new HashSet<>();
}
