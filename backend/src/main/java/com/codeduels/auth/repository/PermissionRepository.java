package com.codeduels.auth.repository;

import com.codeduels.auth.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

}
