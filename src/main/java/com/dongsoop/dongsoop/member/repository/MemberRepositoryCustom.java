package com.dongsoop.dongsoop.member.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.member.dto.LoginMemberDetails;
import com.dongsoop.dongsoop.member.entity.Member;
import java.util.List;
import java.util.Optional;

public interface MemberRepositoryCustom {

    Optional<LoginMemberDetails> findLoginMemberDetailById(Long id);

    long softDelete(Long id, String emailAlias, String passwordAlias);

    List<Member> searchAllByDeviceNotEmpty();

    List<Member> searchAllByDepartmentAndDeviceNotEmpty(Department department);
}
