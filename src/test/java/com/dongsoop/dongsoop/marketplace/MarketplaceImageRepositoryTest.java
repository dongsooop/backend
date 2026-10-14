package com.dongsoop.dongsoop.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoard;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceImage;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceImageRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MarketplaceImageRepositoryTest {

    @Autowired
    private MarketplaceImageRepository marketplaceImageRepository;

    @Autowired
    private MarketplaceBoardRepository marketplaceBoardRepository;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private BoardSearchRepository boardSearchRepository;

    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    @Test
    @DisplayName("이미지를 저장하면 게시글 ID로 개수가 조회되고, 삭제하면 조회되지 않는다")
    void saves_counts_and_soft_deletes_by_board_id() {
        Member author = memberRepository.save(Member.builder()
                .email("market-image@dongyang.ac.kr")
                .nickname("장터작성")
                .password("encoded")
                .build());
        MarketplaceBoard board = marketplaceBoardRepository.save(MarketplaceBoard.builder()
                .title("제목")
                .content("내용")
                .author(author)
                .price(1000L)
                .type(MarketplaceType.values()[0])
                .build());
        marketplaceImageRepository.save(new MarketplaceImage(board, "https://example.test/image.png"));

        assertThat(marketplaceImageRepository.countByMarketplaceBoardId(board.getId())).isEqualTo(1);

        marketplaceImageRepository.deleteByMarketplaceBoardId(board.getId());

        assertThat(marketplaceImageRepository.countByMarketplaceBoardId(board.getId())).isZero();
    }
}
