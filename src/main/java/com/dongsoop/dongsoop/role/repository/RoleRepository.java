package com.dongsoop.dongsoop.role.repository;

import com.dongsoop.dongsoop.role.entity.Role;
import com.dongsoop.dongsoop.role.entity.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoleRepository extends JpaRepository<Role, RoleType> {

    @Query("SELECT r FROM Role r WHERE r.roleType = :roleType")
    Role findByRoleType(RoleType roleType);
}
