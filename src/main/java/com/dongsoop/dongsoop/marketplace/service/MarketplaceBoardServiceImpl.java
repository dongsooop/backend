package com.dongsoop.dongsoop.marketplace.service;

import com.dongsoop.dongsoop.marketplace.dto.CreateMarketplaceBoardRequest;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardDetails;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardOverview;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceViewType;
import com.dongsoop.dongsoop.marketplace.dto.UpdateMarketplaceBoardRequest;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoard;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceImage;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceImage.MarketplaceImageId;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import com.dongsoop.dongsoop.marketplace.exception.MarketplaceBoardImageAmountNotAvailableException;
import com.dongsoop.dongsoop.marketplace.exception.MarketplaceBoardNotFoundException;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepositoryCustom;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceImageRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.s3.S3Service;
import com.dongsoop.dongsoop.s3.exception.S3UnknownException;
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

    private void saveImages(MultipartFile[] images, MarketplaceBoard board) {
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

    public List<MarketplaceBoardOverview> getMarketplaceBoards(Pageable pageable, MarketplaceType type) {
        return marketplaceBoardRepositoryCustom.findMarketplaceBoardOverviewByPage(pageable, type);
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

    @Transactional
    public void delete(Long boardId) {
        marketplaceImageRepository.deleteByMarketplaceBoardId(boardId);
        marketplaceBoardRepository.deleteById(boardId);
    }

    @Override
    @Transactional
    public void update(Long boardId, UpdateMarketplaceBoardRequest request, MultipartFile[] images) throws IOException {
        // 게시글 내용 수정
        MarketplaceBoard board = marketplaceBoardRepository.findById(boardId)
                .orElseThrow(() -> new MarketplaceBoardNotFoundException(boardId));

        board.update(request);

        // 이미지 삭제
        if (request.deleteImageUrls() != null) {
            List<MarketplaceImageId> deleteImage = request.deleteImageUrls()
                    .stream()
                    .map(v -> new MarketplaceImageId(board, v))
                    .toList();
            marketplaceImageRepository.deleteAllById(deleteImage);
        }

        // 이미지 추가
        if (images != null && images.length > 0) {
            saveImages(images, board);
        }

        // 최종 이미지 수 검증
        validateAfterUpdateImageAmount(boardId);
    }

    private void validateAfterUpdateImageAmount(Long boardId) {
        int imageAmount = marketplaceImageRepository.countByMarketplaceBoardId(boardId);
        if (imageAmount > 3) {
            throw new MarketplaceBoardImageAmountNotAvailableException(imageAmount);
        }
    }

    @Override
    @Transactional
    public void close(Long boardId) {
        Long memberId = memberService.getMemberIdByAuthentication();

        MarketplaceBoard board = marketplaceBoardRepository.findByIdAndAuthorId(boardId, memberId)
                .orElseThrow(() -> new MarketplaceBoardNotFoundException(boardId));

        board.close();
    }
}
