package com.dongsoop.dongsoop.marketplace.service;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import com.dongsoop.dongsoop.marketplace.dto.ContactMarketplaceRequest;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoard;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceContact;
import com.dongsoop.dongsoop.marketplace.exception.MarketplaceAlreadyContactException;
import com.dongsoop.dongsoop.marketplace.exception.MarketplaceBoardNotFoundException;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceContactRepository;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceContactRepositoryCustom;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.search.entity.BoardType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketplaceContactServiceImpl implements MarketplaceContactService {

    private final MarketplaceContactRepository marketplaceContactRepository;

    private final MarketplaceContactRepositoryCustom marketplaceContactRepositoryCustom;

    private final MarketplaceBoardRepository marketplaceBoardRepository;

    private final MarketplaceContactMapper marketplaceContactMapper;

    private final MemberService memberService;

    private final ChatRoomService chatRoomService;

    public String contact(ContactMarketplaceRequest request) {
        Long boardId = request.boardId();

        validateBoardExists(boardId);

        Long memberId = memberService.getMemberIdByAuthentication();
        validateAlreadyContact(memberId, boardId);

        String roomId = createMarketplaceChatRoom(memberId, boardId);

        MarketplaceContact marketplaceContact = marketplaceContactMapper.toEntity(request);
        marketplaceContactRepository.save(marketplaceContact);

        return roomId;
    }

    private void validateBoardExists(Long boardId) {
        boolean isExistsBoard = marketplaceBoardRepository.existsById(boardId);
        if (!isExistsBoard) {
            throw new MarketplaceBoardNotFoundException(boardId);
        }
    }

    private void validateAlreadyContact(Long memberId, Long boardId) {
        boolean isAlreadyApplied = marketplaceContactRepositoryCustom.existsByBoardIdAndMemberId(boardId,
                memberId);

        if (isAlreadyApplied) {
            throw new MarketplaceAlreadyContactException(memberId, boardId);
        }
    }

    private String createMarketplaceChatRoom(Long buyerId, Long boardId) {
        SellerIdAndTitle sellerInfo = getSellerIdAndTitleByBoardId(boardId);

        ChatRoom chatRoom = chatRoomService.createContactChatRoom(
                buyerId,
                sellerInfo.sellerId(),
                BoardType.MARKETPLACE,
                boardId,
                sellerInfo.title()
        );
        return chatRoom.getRoomId();
    }

    private SellerIdAndTitle getSellerIdAndTitleByBoardId(Long boardId) {
        MarketplaceBoard board = marketplaceBoardRepository.findById(boardId)
                .orElseThrow(() -> new MarketplaceBoardNotFoundException(boardId));
        return new SellerIdAndTitle(board.getAuthor().getId(), board.getTitle());
    }

    private record SellerIdAndTitle(Long sellerId, String title) {
    }
}
