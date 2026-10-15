package com.dongsoop.dongsoop.marketplace;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import com.dongsoop.dongsoop.marketplace.dto.ContactMarketplaceRequest;
import com.dongsoop.dongsoop.marketplace.entity.*;
import com.dongsoop.dongsoop.marketplace.exception.*;
import com.dongsoop.dongsoop.marketplace.repository.*;
import com.dongsoop.dongsoop.marketplace.service.*;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.search.entity.BoardType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketplaceContactContractTest {
    @Mock MarketplaceContactRepository contacts;
    @Mock MarketplaceContactRepositoryCustom queries;
    @Mock MarketplaceBoardRepository boards;
    @Mock MemberService members;
    @Mock ChatRoomService rooms;
    MarketplaceContactServiceImpl service;
    private final ContactMarketplaceRequest request = new ContactMarketplaceRequest(9L);

    @BeforeEach
    void setup() { service = new MarketplaceContactServiceImpl(contacts, queries, boards, new MarketplaceContactMapper(members), members, rooms); }

    @Test
    void contactCreatesSellerBuyerRoomAndPersistsExactApplicantAndBoard() {
        prepareBoard();
        Member buyer = Member.builder().id(7L).build();
        when(members.getMemberReferenceByContext()).thenReturn(buyer);
        when(rooms.createContactChatRoom(7L, 8L, BoardType.MARKETPLACE, 9L, "book"))
                .thenReturn(ChatRoom.builder().roomId("room-9").build());
        assertThat(service.contact(request)).isEqualTo("room-9");
        ArgumentCaptor<MarketplaceContact> saved = ArgumentCaptor.forClass(MarketplaceContact.class);
        verify(contacts).save(saved.capture());
        assertThat(saved.getValue()).extracting("marketplaceId", "applicant").containsExactly(9L, buyer);
    }

    @Test
    void absentBoardStopsBeforeMemberLookupOrContactWrites() {
        assertThatThrownBy(() -> service.contact(request)).isInstanceOf(MarketplaceBoardNotFoundException.class);
        verifyNoInteractions(members, contacts, queries, rooms);
    }

    @Test
    void duplicateContactCannotCreateAnotherRoomOrSaveContact() {
        when(boards.existsById(9L)).thenReturn(true);
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        when(queries.existsByBoardIdAndMemberId(9L, 7L)).thenReturn(true);
        assertThatThrownBy(() -> service.contact(request)).isInstanceOf(MarketplaceAlreadyContactException.class);
        verifyNoInteractions(rooms, contacts);
    }

    @Test
    void boardDisappearingAfterExistenceCheckHasDomainErrorWithoutRoom() {
        when(boards.existsById(9L)).thenReturn(true);
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        assertThatThrownBy(() -> service.contact(request)).isInstanceOf(MarketplaceBoardNotFoundException.class);
        verifyNoInteractions(rooms, contacts);
    }

    @Test
    void roomFailureNeverPersistsSuccessfulContact() {
        prepareBoard();
        when(rooms.createContactChatRoom(7L, 8L, BoardType.MARKETPLACE, 9L, "book")).thenThrow(new IllegalStateException("unavailable"));
        assertThatThrownBy(() -> service.contact(request)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(contacts);
    }

    private void prepareBoard() {
        when(boards.existsById(9L)).thenReturn(true);
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        when(boards.findById(9L)).thenReturn(Optional.of(MarketplaceBoard.builder().id(9L).author(Member.builder().id(8L).build()).title("book").build()));
    }
}
