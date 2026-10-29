package com.dongsoop.dongsoop.marketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.marketplace.dto.CreateMarketplaceBoardRequest;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardDetails;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardOverview;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceViewType;
import com.dongsoop.dongsoop.marketplace.dto.UpdateMarketplaceBoardRequest;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoard;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoardStatus;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import com.dongsoop.dongsoop.marketplace.exception.MarketplaceBoardAlreadyClosedException;
import com.dongsoop.dongsoop.marketplace.exception.MarketplaceBoardImageAmountNotAvailableException;
import com.dongsoop.dongsoop.marketplace.exception.MarketplaceBoardNotFoundException;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepositoryCustom;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepositoryCustomImpl;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceImageRepository;
import com.dongsoop.dongsoop.marketplace.service.MarketplaceBoardMapper;
import com.dongsoop.dongsoop.marketplace.service.MarketplaceBoardServiceImpl;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.s3.S3Service;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  MarketplacePersistenceServiceTest.QueryConfig.class,
  MarketplaceBoardRepositoryCustomImpl.class
})
class MarketplacePersistenceServiceTest extends AbstractIntegrationTest {
  @Autowired MarketplaceBoardRepository boards;
  @Autowired MarketplaceBoardRepositoryCustom queries;
  @Autowired MarketplaceImageRepository images;
  @Autowired EntityManager em;
  private MemberService members;
  private S3Service s3;
  private Member author;
  private MarketplaceBoardServiceImpl service;

  @TestConfiguration
  static class QueryConfig {
    @Bean
    JPAQueryFactory queryFactory(EntityManager em) {
      return new JPAQueryFactory(em);
    }
  }

  @BeforeEach
  void fixtures() {
    members = mock(MemberService.class);
    s3 = mock(S3Service.class);
    author =
        Member.builder().email("author@example.com").nickname("author").password("encoded").build();
    em.persist(author);
    when(members.getMemberReferenceByContext()).thenReturn(author);
    service =
        new MarketplaceBoardServiceImpl(
            new MarketplaceBoardMapper(members), boards, queries, images, members, s3);
  }

  @Test
  void createAndPartialUpdatePersistWithoutErasingMissingFields() throws Exception {
    MarketplaceBoard board = create("판매", MarketplaceType.SELL, 1);
    Long id = board.getId();
    service.update(id, new UpdateMarketplaceBoardRequest("수정", null, 2000L, null, null), null);
    flushAndClear();
    MarketplaceBoardDetails details =
        queries.findMarketplaceBoardDetails(id, MarketplaceViewType.OWNER).orElseThrow();
    assertThat(details.title()).isEqualTo("수정");
    assertThat(details.content()).isEqualTo("내용");
    assertThat(details.price()).isEqualTo(2000L);
    assertThat(details.type()).isEqualTo(MarketplaceType.SELL);
    assertThat(details.status()).isEqualTo(MarketplaceBoardStatus.OPEN);
    assertThat(details.imageUrlList()).isEmpty();
    assertThat(details.authorId()).isEqualTo(author.getId());
  }

  @Test
  void listingFiltersTypeClosedAndDeletedAndKeepsNewestPage() throws Exception {
    MarketplaceBoard older = create("오래된 판매", MarketplaceType.SELL, 1);
    MarketplaceBoard newer = create("새 판매", MarketplaceType.SELL, 2);
    create("구매", MarketplaceType.BUY, 3);
    MarketplaceBoard closed = create("종료", MarketplaceType.SELL, 4);
    closed.close();
    MarketplaceBoard deleted = create("삭제", MarketplaceType.SELL, 5);
    deleted.setDeleted(true);
    Long olderId = older.getId();
    Long newerId = newer.getId();
    flushAndClear();
    assertThat(service.getMarketplaceBoards(PageRequest.of(0, 1), MarketplaceType.SELL))
        .extracting(MarketplaceBoardOverview::id)
        .containsExactly(newerId);
    assertThat(service.getMarketplaceBoards(PageRequest.of(1, 1), MarketplaceType.SELL))
        .extracting(MarketplaceBoardOverview::id)
        .containsExactly(olderId);
  }

  @Test
  void closingRequiresOwnerAndCannotBeRepeated() throws Exception {
    MarketplaceBoard board = create("판매", MarketplaceType.SELL, 1);
    Long id = board.getId();
    when(members.getMemberIdByAuthentication()).thenReturn(-1L);
    assertThatThrownBy(() -> service.close(id))
        .isInstanceOf(MarketplaceBoardNotFoundException.class);
    when(members.getMemberIdByAuthentication()).thenReturn(author.getId());
    service.close(id);
    flushAndClear();
    assertThat(
            queries
                .findMarketplaceBoardDetails(id, MarketplaceViewType.OWNER)
                .orElseThrow()
                .status())
        .isEqualTo(MarketplaceBoardStatus.CLOSED);
    assertThatThrownBy(() -> service.close(id))
        .isInstanceOf(MarketplaceBoardAlreadyClosedException.class);
  }

  @Test
  void imageAddAndDeletePersistUrls() throws Exception {
    MarketplaceBoard board = create("판매", MarketplaceType.SELL, 1);
    Long id = board.getId();
    prepareUploads();
    UpdateMarketplaceBoardRequest request =
        new UpdateMarketplaceBoardRequest(null, null, null, null, null);
    service.update(id, request, uploadFiles(3));
    flushAndClear();
    assertThat(
            queries
                .findMarketplaceBoardDetails(id, MarketplaceViewType.OWNER)
                .orElseThrow()
                .imageUrlList())
        .containsExactlyInAnyOrder(
            "https://example.com/0.jpg", "https://example.com/1.jpg", "https://example.com/2.jpg");
    service.update(
        id,
        new UpdateMarketplaceBoardRequest(
            null, null, null, List.of("https://example.com/1.jpg"), null),
        null);
    flushAndClear();
    assertThat(
            queries
                .findMarketplaceBoardDetails(id, MarketplaceViewType.OWNER)
                .orElseThrow()
                .imageUrlList())
        .containsExactlyInAnyOrder("https://example.com/0.jpg", "https://example.com/2.jpg");
  }

  @Test
  void imageLimitRejectsFourthStoredImage() throws Exception {
    MarketplaceBoard board = create("판매", MarketplaceType.SELL, 1);
    Long id = board.getId();
    prepareUploads();
    UpdateMarketplaceBoardRequest request =
        new UpdateMarketplaceBoardRequest(null, null, null, null, null);
    MultipartFile[] files = uploadFiles(4);
    assertThatThrownBy(() -> service.update(id, request, files))
        .isInstanceOf(MarketplaceBoardImageAmountNotAvailableException.class);
  }

  private MarketplaceBoard create(String title, MarketplaceType type, int day) throws Exception {
    MarketplaceBoard board =
        service.create(new CreateMarketplaceBoardRequest(title, "내용", 1000L, type), null);
    em.flush();
    em.createNativeQuery("update marketplace_board set created_at = :createdAt where id = :id")
        .setParameter("createdAt", LocalDateTime.of(2026, 10, day, 12, 0))
        .setParameter("id", board.getId())
        .executeUpdate();
    return board;
  }

  private void prepareUploads() throws Exception {
    when(s3.upload(any(), eq("marketplace/images"), anyLong()))
        .thenAnswer(
            call ->
                "https://example.com/"
                    + ((MultipartFile) call.getArgument(0)).getOriginalFilename());
  }

  private MultipartFile[] uploadFiles(int count) {
    MultipartFile[] files = new MultipartFile[count];
    for (int index = 0; index < count; index++) {
      files[index] =
          new MockMultipartFile("image", index + ".jpg", "image/jpeg", new byte[] {1, 2});
    }
    return files;
  }

  private void flushAndClear() {
    em.flush();
    em.clear();
  }
}
