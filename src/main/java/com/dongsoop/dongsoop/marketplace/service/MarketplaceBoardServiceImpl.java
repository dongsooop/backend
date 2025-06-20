package com.dongsoop.dongsoop.marketplace.service;

import com.dongsoop.dongsoop.exception.domain.member.MemberNotFoundException;
import com.dongsoop.dongsoop.exception.domain.s3.S3UnknownException;
import com.dongsoop.dongsoop.marketplace.dto.CreateMarketplaceBoardRequest;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardDetails;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardOverview;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceViewType;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoard;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceImage;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceImage.MarketplaceImageId;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepositoryCustom;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceImageRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.s3.S3Service;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MarketplaceBoardServiceImpl implements MarketplaceBoardService {

    private static final String DIRECTORY_PATH = "marketplace/images";

    private final MarketplaceBoardMapper marketplaceBoardMapper;

    private final MarketplaceBoardRepository marketplaceBoardRepository;

    private final MarketplaceBoardRepositoryCustom marketplaceBoardRepositoryCustom;

    private final MarketplaceImageRepository marketplaceImageRepository;

    private final MemberService memberService;

    private final S3Service s3Service;

    @Transactional
    public MarketplaceBoard create(CreateMarketplaceBoardRequest request, MultipartFile[] images) throws IOException {
        MarketplaceBoard board = marketplaceBoardMapper.toEntity(request);
        MarketplaceBoard savedBoard = marketplaceBoardRepository.save(board);

        if (images != null && images.length > 0) {
            saveImages(images, savedBoard);
        }

        return savedBoard;
    }

    private void saveImages(MultipartFile[] images, MarketplaceBoard board) throws IOException {
        List<MarketplaceImage> imageLinkList = Arrays.stream(images)
                .map(image -> uploadImage(image, board))
                .toList();

        marketplaceImageRepository.saveAll(imageLinkList);
    }

    private MarketplaceImage uploadImage(MultipartFile image, MarketplaceBoard board) {
        try {
            String url = s3Service.upload(image, DIRECTORY_PATH, board.getId());
            MarketplaceImageId id = new MarketplaceImageId(board, url);
            return new MarketplaceImage(id);
        } catch (IOException exception) {
            throw new S3UnknownException(exception);
        }
    }

    public List<MarketplaceBoardOverview> getMarketplaceBoards(Pageable pageable) {
        return marketplaceBoardRepositoryCustom.findMarketplaceBoardOverviewByPage(pageable);
    }

    public MarketplaceBoardDetails getBoardDetails(Long boardId) {
        try {
            Member member = memberService.getMemberReferenceByContext();
            boolean isOwner = marketplaceBoardRepository.existsByIdAndAuthor(boardId, member);
            if (isOwner) {
                return getBoardDetailsWithViewType(boardId, MarketplaceViewType.OWNER);
            }

            return getBoardDetailsWithViewType(boardId, MarketplaceViewType.MEMBER);
        } catch (MemberNotFoundException exception) {
            return getBoardDetailsWithViewType(boardId, MarketplaceViewType.GUEST);
        }
    }

    public MarketplaceBoardDetails getBoardDetailsWithViewType(Long boardId, MarketplaceViewType viewType) {
        return marketplaceBoardRepositoryCustom.findMarketplaceBoardDetails(boardId, viewType)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    }
}
