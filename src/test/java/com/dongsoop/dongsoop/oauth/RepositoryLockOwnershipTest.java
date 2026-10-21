package com.dongsoop.dongsoop.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryLockOwnershipTest.Config.class)
class RepositoryLockOwnershipTest extends AbstractIntegrationTest {
    @Autowired EntityManager em;
    @Autowired MemberRepository members;
    @Autowired MemberDeviceRepository devices;
    @Autowired PlatformTransactionManager transactions;

    @TestConfiguration
    static class Config {
        @Bean JPAQueryFactory queryFactory(EntityManager em) { return new JPAQueryFactory(em); }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void ownerRepositoryRetainsRowLockUntilCallingTransactionEnds(boolean memberLock) throws Exception {
        var tx = new TransactionTemplate(transactions);
        Long id = tx.execute(status -> {
            Member member = Member.builder().email("lock-" + memberLock + "@example.test")
                    .nickname("l" + memberLock).password("encoded").build();
            em.persist(member);
            MemberDevice device = MemberDevice.builder().member(member).memberDeviceType(MemberDeviceType.IOS)
                    .deviceToken("lock-" + memberLock).build();
            em.persist(device);
            em.flush();
            if (memberLock) {
                return member.getId();
            }
            return device.getId();
        });
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        var holder = executor.submit(() -> tx.executeWithoutResult(status -> {
            lock(memberLock, id);
            locked.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("lock release was not signalled");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }));
        try {
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                em.createNativeQuery("SET LOCAL lock_timeout = '250ms'").executeUpdate();
                lock(memberLock, id);
            })).hasRootCauseInstanceOf(SQLException.class).hasStackTraceContaining("lock timeout");
        } finally {
            release.countDown();
            try {
                holder.get(10, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
            }
        }
        tx.executeWithoutResult(status -> lock(memberLock, id));
    }

    private void lock(boolean memberLock, Long id) {
        if (memberLock) {
            members.findAndLockById(id);
            return;
        }
        devices.lockById(id);
    }
}
