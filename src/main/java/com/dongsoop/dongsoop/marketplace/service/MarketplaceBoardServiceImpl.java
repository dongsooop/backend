package com.dongsoop.dongsoop.marketplace.service;

import com.dongsoop.dongsoop.marketplace.dto.CreateMarketplaceBoardRequest;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardOverview;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoard;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceImage;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceImage.MarketplaceImageId;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepositoryCustom;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceImageRepository;
import com.dongsoop.dongsoop.s3.S3Service;
import java.io.IOException;
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

    private final S3Service s3Service;

    @Transactional
    public MarketplaceBoard create(CreateMarketplaceBoardRequest request, MultipartFile image) throws IOException {
        MarketplaceBoard board = marketplaceBoardMapper.toEntity(request);
        MarketplaceBoard result = marketplaceBoardRepository.save(board);

        String url = s3Service.upload(image, DIRECTORY_PATH, board.getId());
        MarketplaceImageId id = new MarketplaceImageId(result, url);
        MarketplaceImage marketplaceImage = new MarketplaceImage(id);

        marketplaceImageRepository.save(marketplaceImage);

        return result;
    }

    public List<MarketplaceBoardOverview> getMarketplaceBoards(Pageable pageable) {
        return marketplaceBoardRepositoryCustom.findMarketplaceBoardOverviewByPage(pageable);
    }
}
