package com.dongsoop.dongsoop.mypage;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoardStatus;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceSummary;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepositoryCustom;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.mypage.controller.MyPageController;
import com.dongsoop.dongsoop.mypage.service.MyPageServiceImpl;
import com.dongsoop.dongsoop.recruitment.RecruitmentType;
import com.dongsoop.dongsoop.recruitment.query.RecruitmentQuery;
import com.dongsoop.dongsoop.recruitment.query.dto.RecruitmentStatusType;
import com.dongsoop.dongsoop.recruitment.query.dto.RecruitmentSummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MyPageQueryHttpContractTest {
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 18, 12, 0);
    private static final PageRequest PAGE = PageRequest.of(1, 2);
    @Mock MemberService members;
    @Mock MarketplaceBoardRepositoryCustom marketplace;
    @Mock RecruitmentQuery recruitment;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var service = new MyPageServiceImpl(members, marketplace, recruitment);
        mvc = MockMvcBuilders.standaloneSetup(new MyPageController(service))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
    }

    @Test
    void appliedRecruitmentPreservesProjectionResponseAndRequestedPage() throws Exception {
        when(recruitment.findApplyRecruitmentsByMemberId(7L, PAGE)).thenReturn(List.of(summary()));

        mvc.perform(get("/mypage/apply-recruitments").param("page", "1").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].volunteer").value(3))
                .andExpect(jsonPath("$[0].title").value("title"))
                .andExpect(jsonPath("$[0].content").value("content"))
                .andExpect(jsonPath("$[0].tags").value("java,spring"))
                .andExpect(jsonPath("$[0].departmentTypeList").value("department"))
                .andExpect(jsonPath("$[0].boardType").value("PROJECT"))
                .andExpect(jsonPath("$[0].status").value("RECRUITING"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].startAt").exists())
                .andExpect(jsonPath("$[0].endAt").exists());
    }

    @Test
    void openedRecruitmentRetainsArrayConversionAtHttpBoundary() throws Exception {
        when(recruitment.findOpenedRecruitmentsByMemberId(7L, PAGE)).thenReturn(List.of(summary()));

        mvc.perform(get("/mypage/opened-recruitments").param("page", "1").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].tags[0]").value("java"))
                .andExpect(jsonPath("$[0].tags[1]").value("spring"))
                .andExpect(jsonPath("$[0].departmentTypeList[0]").value("department"))
                .andExpect(jsonPath("$[0].boardType").value("PROJECT"))
                .andExpect(jsonPath("$[0].status").value("RECRUITING"));
    }

    @Test
    void marketplaceMapsQuerySummaryToExistingResponseFields() throws Exception {
        var row = new MarketplaceSummary(12L, "title", "content", 1000L, CREATED,
                2L, "image", MarketplaceType.SELL, MarketplaceBoardStatus.OPEN);
        when(marketplace.findOpenedMarketplaceByAuthorIdAndPage(7L, PAGE)).thenReturn(List.of(row));

        mvc.perform(get("/mypage/opened-marketplace").param("page", "1").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].title").value("title"))
                .andExpect(jsonPath("$[0].content").value("content"))
                .andExpect(jsonPath("$[0].price").value(1000))
                .andExpect(jsonPath("$[0].contactCount").value(2))
                .andExpect(jsonPath("$[0].imageUrl").value("image"))
                .andExpect(jsonPath("$[0].type").value("SELL"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    private RecruitmentSummary summary() {
        return new SpelAwareProxyProjectionFactory().createProjection(RecruitmentSummary.class,
                Map.ofEntries(Map.entry("id", 11L), Map.entry("volunteer", 3L),
                        Map.entry("startAt", CREATED), Map.entry("endAt", CREATED.plusDays(1)),
                        Map.entry("title", "title"), Map.entry("content", "content"),
                        Map.entry("tags", "java,spring"), Map.entry("departmentTypeList", "department"),
                        Map.entry("boardType", RecruitmentType.PROJECT), Map.entry("createdAt", CREATED),
                        Map.entry("status", RecruitmentStatusType.RECRUITING)));
    }
}
