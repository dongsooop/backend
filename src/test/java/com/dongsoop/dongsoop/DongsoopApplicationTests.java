package com.dongsoop.dongsoop;

import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class DongsoopApplicationTests {

    // elasticsearch를 연결하지 않은 상태에서 테스트하기 위해 MockitoBean 사용
    @MockitoBean
    private BoardSearchRepository boardSearchRepository;

    @Test
    void contextLoads() {
    }

}
