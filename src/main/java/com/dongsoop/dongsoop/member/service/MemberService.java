package com.dongsoop.dongsoop.member.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.department.service.DepartmentService;
import com.dongsoop.dongsoop.exception.domain.member.EmailDuplicatedException;
import com.dongsoop.dongsoop.exception.domain.member.MemberNotFoundException;
import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.jwt.dto.TokenIssueResponse;
import com.dongsoop.dongsoop.member.dto.LoginAuthenticate;
import com.dongsoop.dongsoop.member.dto.LoginRequest;
import com.dongsoop.dongsoop.member.dto.SignupRequest;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.role.entity.MemberRole;
import com.dongsoop.dongsoop.role.entity.Role;
import com.dongsoop.dongsoop.role.entity.RoleType;
import com.dongsoop.dongsoop.role.repository.MemberRoleRepository;
import com.dongsoop.dongsoop.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    private final RoleRepository roleRepository;

    private final MemberRoleRepository memberRoleRepository;

    private final DepartmentRepository departmentRepository;

    private final DepartmentService departmentService;

    private final PasswordEncoder passwordEncoder;

    private final TokenGenerator tokenGenerator;

    @Transactional
    public void signup(SignupRequest request) {
        checkEmailDuplication(request.getEmail());

        Member member = transformToMemberBySignupRequest(request);
        memberRepository.save(member);

        Role userRole = roleRepository.findByRoleType(RoleType.USER);
        MemberRole memberRole = new MemberRole(member, userRole);
        memberRoleRepository.save(memberRole);
    }

    private Member transformToMemberBySignupRequest(SignupRequest request) {
        String email = request.getEmail();
        String nickname = request.getNickname();
        String studentId = request.getStudentId();
        DepartmentType departmentType = request.getDepartmentType();

        Department proxyDepartment = departmentService.getReferenceById(departmentType);

        return Member.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(nickname)
                .studentId(studentId)
                .department(proxyDepartment)
                .build();
    }

    public TokenIssueResponse login(LoginRequest loginRequest) {
        LoginAuthenticate loginAuthenticate = getLoginAuthenticate(loginRequest.getEmail());

        String password = loginAuthenticate.getPassword();
        validatePassword(loginRequest.getPassword(), password);

        Authentication authentication = getAuthenticationByLoginAuthenticate(loginAuthenticate);

        String accessToken = tokenGenerator.generateAccessToken(authentication);
        String refreshToken = tokenGenerator.generateRefreshToken(authentication);

        return new TokenIssueResponse(accessToken, refreshToken);
    }

    private Authentication getAuthenticationByLoginAuthenticate(LoginAuthenticate loginAuthenticate) {
        Long id = loginAuthenticate.getId();
        List<Role> roleList = memberRoleRepository.findAllByMemberId(id);
        Collection<GrantedAuthority> role = getAuthoritiesByRoleList(roleList);

        return new UsernamePasswordAuthenticationToken(id, null, role);
    }

    private Collection<GrantedAuthority> getAuthoritiesByRoleList(List<Role> roleList) {
        return roleList.stream()
                .map(Role::getRoleType)
                .map(RoleType::getAuthority)
                .toList();
    }

    private LoginAuthenticate getLoginAuthenticate(String email) {
        Optional<LoginAuthenticate> optionalAuthenticate = memberRepository.findLoginAuthenticateByEmail(email);
        return optionalAuthenticate.orElseThrow(MemberNotFoundException::new);
    }

    public LoginAuthenticate getLoginAuthenticateByNickname(String nickname) {
        Optional<LoginAuthenticate> optionalAuthenticate = memberRepository.findLoginAuthenticateByNickname(nickname);
        return optionalAuthenticate.orElseThrow(MemberNotFoundException::new);
    }

    private void validatePassword(String loginPassword, String password) {
        if (!passwordEncoder.matches(loginPassword, password)) {
            throw new MemberNotFoundException();
        }
    }

    private void checkEmailDuplication(String email) {
        boolean isExists = memberRepository.existsByEmail(email);

        if (isExists) {
            throw new EmailDuplicatedException();
        }
    }
}
