package com.dongsoop.dongsoop.marketplace.service;

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

    public void contact(ContactMarketplaceRequest request) {
        Long boardId = request.boardId();

        validateBoardExists(boardId);

        Long memberId = memberService.getMemberIdByAuthentication();
        validateAlreadyContact(memberId, boardId);

        createMarketplaceChatRoom(memberId, boardId);

        MarketplaceContact marketplaceContact = marketplaceContactMapper.toEntity(request);
        marketplaceContactRepository.save(marketplaceContact);
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

    private void createMarketplaceChatRoom(Long buyerId, Long boardId) {
        Long sellerId = getSellerIdByBoardId(boardId);
        String boardTitle = getBoardTitleById(boardId);

        chatRoomService.createContactChatRoom(buyerId, sellerId, null, boardId, boardTitle);
    }

    private Long getSellerIdByBoardId(Long boardId) {
        return marketplaceBoardRepository.findById(boardId)
                .map(board -> board.getAuthor().getId())
                .orElseThrow(() -> new MarketplaceBoardNotFoundException(boardId));
    }

    private String getBoardTitleById(Long boardId) {
        return marketplaceBoardRepository.findById(boardId)
                .map(MarketplaceBoard::getTitle)
                .orElseThrow(() -> new MarketplaceBoardNotFoundException(boardId));
    }
}
