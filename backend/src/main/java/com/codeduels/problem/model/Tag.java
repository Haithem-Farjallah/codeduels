package com.codeduels.problem.model;

import com.codeduels.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tag")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Tag extends BaseEntity {

    @Column(nullable = false,unique = true)
    private String name;


}
