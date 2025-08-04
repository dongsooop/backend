package com.dongsoop.dongsoop.memberblock.aspect;

import com.dongsoop.dongsoop.member.service.MemberService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class MemberBlockFilterAspect {

    private final MemberService memberService;  // 조회자 정보 제공

    @PersistenceContext
    private EntityManager em;

    @Around("@annotation(com.dongsoop.dongsoop.memberblock.annotation.ApplyBlockFilter)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Long blockerId = memberService.getMemberIdByAuthentication();
        Session session = em.unwrap(Session.class);

        // 필터 활성화
        session.enableFilter("blockFilter")
                .setParameter("blockerId", blockerId);

        try {
            return pjp.proceed();
        } finally {
            // 메서드 종료 후 필터 비활성화
            session.disableFilter("blockFilter");
        }
    }
}
