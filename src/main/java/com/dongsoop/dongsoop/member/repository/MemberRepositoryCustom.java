package com.dongsoop.dongsoop.member.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.member.dto.DeleteMember;
import com.dongsoop.dongsoop.member.dto.LoginMemberDetails;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;
import java.util.Optional;

public interface MemberRepositoryCustom {

    Optional<LoginMemberDetails> findLoginMemberDetailById(Long id);

    long softDelete(DeleteMember deleteMember);

    List<Member> searchAllByDeviceNotEmpty();

    List<Member> searchAllByDepartmentAndDeviceNotEmpty(Department department);

    List<Member> findByRoleTypeWithDevice(RoleType roleType);
}
