package com.dongsoop.dongsoop.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.common.exception.authentication.NotAuthenticationException;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.service.DepartmentService;
import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.member.dto.DeleteMember;
import com.dongsoop.dongsoop.member.dto.SignupRequest;
import com.dongsoop.dongsoop.member.dto.UpdatePasswordRequest;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberServiceImpl;
import com.dongsoop.dongsoop.member.validate.MemberDuplicationValidator;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.oauth.service.OAuth2Service;
import com.dongsoop.dongsoop.report.validator.ReportValidator;
import com.dongsoop.dongsoop.role.entity.Role;
import com.dongsoop.dongsoop.role.entity.RoleType;
import com.dongsoop.dongsoop.role.repository.MemberRoleRepository;
import com.dongsoop.dongsoop.role.repository.RoleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberLifecycleServiceTest {
    @Mock MemberRepository members;
    @Mock RoleRepository roles;
    @Mock MemberRoleRepository memberRoles;
    @Mock DepartmentService departments;
    @Mock PasswordEncoder encoder;
    @Mock TokenGenerator tokens;
    @Mock MemberDuplicationValidator duplicates;
    @Mock ReportValidator reports;
    @Mock MemberDeviceRepository devices;
    @Mock MemberDeviceService deviceService;
    @Mock OAuth2Service oauth;
    @InjectMocks MemberServiceImpl service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void signupStoresEncodedPasswordAndDepartment() {
        Department department = mock(Department.class);
        Role role = mock(Role.class);
        when(departments.getReferenceById(DepartmentType.DEPT_2001)).thenReturn(department);
        when(encoder.encode("raw-password")).thenReturn("encoded-password");
        when(roles.findByRoleType(RoleType.USER)).thenReturn(role);
        SignupRequest request = signupRequest();

        service.signup(request);

        ArgumentCaptor<Member> stored = ArgumentCaptor.forClass(Member.class);
        verify(members).save(stored.capture());
        assertThat(stored.getValue().getEmail()).isEqualTo("member@example.com");
        assertThat(stored.getValue().getNickname()).isEqualTo("회원");
        assertThat(stored.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(stored.getValue().getDepartment()).isSameAs(department);
        verify(duplicates).validateEmailDuplication(request.getEmail());
        verify(duplicates).validateNicknameDuplication(request.getNickname());
        verify(memberRoles).save(any());
    }

    @Test
    void duplicateEmailStopsSignupBeforeSaving() {
        doThrow(new IllegalStateException("duplicate")).when(duplicates)
                .validateEmailDuplication("member@example.com");
        SignupRequest request = signupRequest();
        assertThatThrownBy(() -> service.signup(request)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(members, roles, memberRoles, encoder);
    }

    @Test
    void duplicateNicknameStopsSignupBeforeSaving() {
        doThrow(new IllegalStateException("duplicate")).when(duplicates).validateNicknameDuplication("회원");
        SignupRequest request = signupRequest();
        assertThatThrownBy(() -> service.signup(request)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(members, roles, memberRoles, encoder);
    }

    @Test
    void passwordChangeUpdatesExistingMember() {
        Member member = Member.builder().email("member@example.com").password("old").build();
        when(members.findByEmail(member.getEmail())).thenReturn(Optional.of(member));
        when(encoder.encode("new-password")).thenReturn("new-encoded");
        UpdatePasswordRequest request = new UpdatePasswordRequest(member.getEmail(), "new-password");
        service.updatePassword(request);
        assertThat(member.getPassword()).isEqualTo("new-encoded");
    }

    @Test
    void missingMemberCannotChangePassword() {
        UpdatePasswordRequest request = new UpdatePasswordRequest("missing@example.com", "new-password");
        assertThatThrownBy(() -> service.updatePassword(request)).isInstanceOf(MemberNotFoundException.class);
        verifyNoInteractions(encoder);
    }

    @Test
    void deletionUnlinksAccountAndReleasesDevices() {
        authenticate("7");
        Member member = Member.builder().id(7L).build();
        MemberDevice device = MemberDevice.builder().member(member).build();
        when(encoder.encode(anyString())).thenReturn("anonymized");
        when(members.softDelete(any())).thenReturn(1L);
        when(devices.findByMemberId(7L)).thenReturn(List.of(device));
        service.deleteMember();
        ArgumentCaptor<DeleteMember> deletion = ArgumentCaptor.forClass(DeleteMember.class);
        verify(members).softDelete(deletion.capture());
        assertThat(deletion.getValue().memberId()).isEqualTo(7L);
        assertThat(deletion.getValue().passwordAlias()).isEqualTo("anonymized");
        ArgumentCaptor<String> rawAlias = ArgumentCaptor.forClass(String.class);
        verify(encoder).encode(rawAlias.capture());
        assertThat(rawAlias.getValue()).matches("[0-9a-f]{32}");
        assertThat(device.getMember()).isNull();
        verify(oauth).unlinkMember(7L);
    }

    @Test
    void missingDeletionStopsDeviceChanges() {
        authenticate("7");
        when(encoder.encode(anyString())).thenReturn("anonymized");
        assertThatThrownBy(service::deleteMember).isInstanceOf(MemberNotFoundException.class);
        verifyNoInteractions(devices);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "anonymousUser", "-7", "7x"})
    void invalidAuthenticationNameIsRejected(String name) {
        authenticate(name);
        assertThatThrownBy(service::getMemberIdByAuthentication).isInstanceOf(NotAuthenticationException.class);
    }

    @Test
    void absentAuthenticationIsRejected() {
        assertThatThrownBy(service::getMemberIdByAuthentication).isInstanceOf(NotAuthenticationException.class);
        assertThat(service.isAuthenticated()).isFalse();
    }

    @Test
    void validContextResolvesMemberReference() {
        authenticate("7");
        Member member = Member.builder().id(7L).nickname("회원").build();
        when(members.getReferenceById(7L)).thenReturn(member);
        when(members.findById(7L)).thenReturn(Optional.of(member));
        assertThat(service.getMemberIdByAuthentication()).isEqualTo(7L);
        assertThat(service.getMemberReferenceByContext()).isSameAs(member);
        assertThat(service.getNicknameById(7L)).isEqualTo("회원");
        assertThat(service.isAuthenticated()).isTrue();
    }

    @Test
    void missingNicknameAndInvalidReferenceAreRejected() {
        authenticate("anonymousUser");
        assertThatThrownBy(service::getMemberReferenceByContext).isInstanceOf(MemberNotFoundException.class);
        assertThatThrownBy(() -> service.getNicknameById(7L)).isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void anonymousTokenIsNotAuthenticated() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertThat(service.isAuthenticated()).isFalse();
    }

    private void authenticate(String name) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(name, null, List.of()));
    }

    private SignupRequest signupRequest() {
        return SignupRequest.builder().email("member@example.com").nickname("회원")
                .password("raw-password").departmentType(DepartmentType.DEPT_2001).build();
    }
}
