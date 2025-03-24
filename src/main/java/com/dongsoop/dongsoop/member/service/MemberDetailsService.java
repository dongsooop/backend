package com.dongsoop.dongsoop.member.service;

import com.dongsoop.dongsoop.exception.domain.member.MemberNotFoundException;
import com.dongsoop.dongsoop.member.dto.MemberDetailsDto;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.entity.MemberDetails;
import com.dongsoop.dongsoop.member.entity.Role;
import com.dongsoop.dongsoop.member.repository.MemberRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);

        Role role = member.getRole();

        List<SimpleGrantedAuthority> authorities = Stream.of(role.toString())
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new User(email, member.getPassword(), authorities);
    }
}
