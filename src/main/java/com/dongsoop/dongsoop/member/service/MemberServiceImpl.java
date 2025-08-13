package com.dongsoop.dongsoop.member.service;

import com.dongsoop.dongsoop.common.exception.authentication.NotAuthenticationException;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.service.DepartmentService;
import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.jwt.dto.IssuedToken;
import com.dongsoop.dongsoop.member.dto.LoginAuthenticate;
import com.dongsoop.dongsoop.member.dto.LoginDetails;
import com.dongsoop.dongsoop.member.dto.LoginMemberDetails;
import com.dongsoop.dongsoop.member.dto.LoginRequest;
import com.dongsoop.dongsoop.member.dto.SignupRequest;
import com.dongsoop.dongsoop.member.dto.UpdatePasswordRequest;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.InvalidPasswordFormatException;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.repository.MemberRepositoryCustom;
import com.dongsoop.dongsoop.member.validate.MemberDuplicationValidator;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.report.validator.ReportValidator;
import com.dongsoop.dongsoop.role.entity.MemberRole;
import com.dongsoop.dongsoop.role.entity.Role;
import com.dongsoop.dongsoop.role.entity.RoleType;
import com.dongsoop.dongsoop.role.repository.MemberRoleRepository;
import com.dongsoop.dongsoop.role.repository.RoleRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final MemberRepositoryCustom memberRepositoryCustom;
    private final DepartmentService departmentService;
    private final PasswordEncoder passwordEncoder;
    private final TokenGenerator tokenGenerator;
    private final MemberDuplicationValidator memberDuplicationValidator;
    private final ReportValidator reportValidator;
    private final MemberDeviceService memberDeviceService;

    @Override
    @Transactional
    public void signup(SignupRequest request) {
        memberDuplicationValidator.validateEmailDuplication(request.getEmail());
        memberDuplicationValidator.validateNicknameDuplication(request.getNickname());

        Member member = transformToMemberBySignupRequest(request);
        memberRepository.save(member);

        Role userRole = roleRepository.findByRoleType(RoleType.USER);
        MemberRole memberRole = new MemberRole(member, userRole);
        memberRoleRepository.save(memberRole);
    }

    private Member transformToMemberBySignupRequest(SignupRequest request) {
        String email = request.getEmail();
        String nickname = request.getNickname();
        DepartmentType departmentType = request.getDepartmentType();

        Department proxyDepartment = departmentService.getReferenceById(departmentType);

        return Member.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(nickname)
                .department(proxyDepartment)
                .build();
    }

    @Override
    @Transactional
    public LoginDetails login(LoginRequest loginRequest) {
        validateMemberExists(loginRequest.getEmail());
        LoginAuthenticate loginAuthenticate = getLoginAuthenticate(loginRequest.getEmail());

        String password = loginAuthenticate.getPassword();
        validatePassword(loginRequest.getPassword(), password);

        reportValidator.checkMemberAccessById(loginAuthenticate.getId());

        Authentication authentication = getAuthenticationByLoginAuthenticate(loginAuthenticate);

        String accessToken = tokenGenerator.generateAccessToken(authentication);
        String refreshToken = tokenGenerator.generateRefreshToken(authentication);

        IssuedToken issuedToken = new IssuedToken(accessToken, refreshToken);
        LoginMemberDetails loginMemberDetails = memberRepositoryCustom.findLoginMemberDetailById(
                        loginAuthenticate.getId())
                .orElseThrow(MemberNotFoundException::new);

        // 사용자 디바이스 등록
        memberDeviceService.registerDeviceByMemberId(loginAuthenticate.getId(), loginRequest.getFcmToken(),
                loginRequest.getDeviceType());

        return new LoginDetails(loginMemberDetails, issuedToken);
    }

    private void validateMemberExists(String email) {
        boolean isMemberExists = memberRepository.existsByEmailAndIsDeletedFalse(email);
        if (!isMemberExists) {
            throw new MemberNotFoundException();
        }
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

    @Override
    public Member getMemberReferenceByContext() {
        Long id = getMemberIdByContext();
        return memberRepository.getReferenceById(id);
    }

    private Long getMemberIdByContext() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        String id = authentication.getName();
        if (StringUtils.hasText(id) && id.matches("\\d+")) {
            return Long.valueOf(id);
        }

        throw new MemberNotFoundException();
    }

    private void validatePassword(String loginPassword, String password) {
        if (!passwordEncoder.matches(loginPassword, password)) {
            throw new InvalidPasswordFormatException();
        }
    }

    @Override
    public String getNicknameById(Long userId) {
        return memberRepository.findById(userId)
                .map(Member::getNickname)
                .orElseThrow(MemberNotFoundException::new);
    }

    @Override
    public Long getMemberIdByAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new NotAuthenticationException();
        }

        String id = authentication.getName();
        if (StringUtils.hasText(id) && id.matches("\\d+")) {
            return Long.valueOf(id);
        }

        throw new NotAuthenticationException();
    }

    @Override
    @Transactional
    public void deleteMember() {
        // 요청 사용자 id
        Long requesterId = getMemberIdByAuthentication();

        // 가명처리
        String emailAlias = generateEmailAlias();
        String passwordAlias = generatePasswordAlias();
        long updatedCount = memberRepositoryCustom.softDelete(requesterId, emailAlias, passwordAlias);
        if (updatedCount == 0L) {
            log.error("Member with id {} not found or already deleted", requesterId);
            throw new MemberNotFoundException();
        }
    }

    private String generateEmailAlias() {
        return generateRawUUID();
    }

    private String generatePasswordAlias() {
        String uuid = generateRawUUID();

        return passwordEncoder.encode(uuid);
    }

    private String generateRawUUID() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "");
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updatePassword(UpdatePasswordRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(MemberNotFoundException::new);

        member.updatePassword(passwordEncoder.encode(request.password()));
    }
}
