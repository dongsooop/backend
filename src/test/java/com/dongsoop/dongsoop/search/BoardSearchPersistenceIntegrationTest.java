package com.dongsoop.dongsoop.search;

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.dongsoop.dongsoop.search.entity.BoardDocument;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepositoryImpl;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class BoardSearchPersistenceIntegrationTest {
    @Container
    static final GenericContainer<?> elasticsearch = new GenericContainer<>("docker.elastic.co/elasticsearch/elasticsearch:8.17.3")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m")
            .withCommand("bash", "-c", "bin/elasticsearch-plugin install --batch analysis-nori && exec /usr/local/bin/docker-entrypoint.sh")
            .withExposedPorts(9200)
            .waitingFor(Wait.forHttp("/").forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(4));
    private static ElasticsearchTransport transport;
    private static ElasticsearchOperations operations;
    private BoardSearchRepositoryImpl repository;

    @BeforeAll
    static void connect() {
        RestClient rest = RestClient.builder(new HttpHost(elasticsearch.getHost(), elasticsearch.getMappedPort(9200))).build();
        transport = new RestClientTransport(rest, new JacksonJsonpMapper());
        operations = new ElasticsearchTemplate(new ElasticsearchClient(transport));
    }

    @AfterAll
    static void close() throws IOException {
        if (transport != null) {
            transport.close();
        }
    }

    @BeforeEach
    void prepareMappingAndDocuments() {
        IndexOperations index = operations.indexOps(BoardDocument.class);
        if (index.exists()) {
            index.delete();
        }
        index.createWithMapping();
        operations.save(List.of(document("1", "NOTICE", null, "software"),
                document("2", "MARKETPLACE", "SELL", "software"),
                document("3", "MARKETPLACE", "BUY", "other"),
                document("4", "PROJECT", null, "software")));
        index.refresh();
        repository = new BoardSearchRepositoryImpl(operations);
    }

    @Test
    void combinedFiltersSelectOnlyMatchingBoardAndKeepPayload() {
        Page<BoardDocument> result = repository.searchDynamic("campus", List.of("MARKETPLACE"), "SELL",
                "software", "writer", PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).singleElement().satisfies(document -> {
            assertThat(document.getId()).isEqualTo("2");
            assertThat(document.getTitle()).isEqualTo("campus 2");
            assertThat(document.getBoardType()).isEqualTo("MARKETPLACE");
            assertThat(document.getMarketplaceType()).isEqualTo("SELL");
            assertThat(document.getPrice()).isEqualTo(1000L);
        });
    }

    @Test
    void noOptionalFiltersKeepTotalAndPageSort() {
        Page<BoardDocument> result = repository.searchDynamic(null, null, null, null, null,
                PageRequest.of(1, 1, Sort.by("boardId")));
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getContent()).extracting(BoardDocument::getId).containsExactly("2");
        assertThat(repository.searchDynamic("nonexistent", List.of(), null, null, null, PageRequest.of(0, 10)))
                .isEmpty();
    }

    @Test
    void autocompleteRestrictsBoardTypeAndNoticeAuthor() {
        assertThat(repository.findAutocompleteSuggestionsDynamic("campus", "MARKETPLACE", PageRequest.of(0, 10)))
                .extracting(BoardDocument::getId).containsExactlyInAnyOrder("2", "3");
        assertThat(repository.findNoticeAutocompleteSuggestionsDynamic("campus", "writer", PageRequest.of(0, 10)))
                .extracting(BoardDocument::getId).containsExactly("1");
        assertThat(repository.findNoticeAutocompleteSuggestionsDynamic("campus", "other-author", PageRequest.of(0, 10)))
                .isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void missingAutocompleteKeywordReturnsNoSuggestions(String keyword) {
        assertThat(repository.findAutocompleteSuggestionsDynamic(keyword, null, PageRequest.of(0, 10))).isEmpty();
        assertThat(repository.findNoticeAutocompleteSuggestionsDynamic(keyword, null, PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    void restaurantSearchFiltersMixedIndexAndRequiresAllTerms() throws IOException {
        seedRestaurants();
        var restaurants = new com.dongsoop.dongsoop.search.repository.RestaurantSearchRepositoryImpl(operations);
        var result = restaurants.searchByKeywordDynamic("campus noodles", PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).singleElement().satisfies(found -> {
            assertThat(found.getRestaurantId()).isEqualTo(101L);
            assertThat(found.getName()).isEqualTo("campus noodles");
            assertThat(found.getCategory()).isEqualTo("food");
            assertThat(found.getPlaceUrl()).isEqualTo("https://example.test/restaurant/101");
        });
        assertThat(restaurants.searchByKeywordDynamic("campus nonexistent", PageRequest.of(0, 10))).isEmpty();
        var page = restaurants.searchByKeywordDynamic(null, PageRequest.of(1, 1, Sort.by("restaurantId")));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(com.dongsoop.dongsoop.search.entity.RestaurantDocument::getRestaurantId)
                .containsExactly(102L);
    }

    @Test
    void restaurantAutocompleteExcludesOtherBoardTypes() throws IOException {
        seedRestaurants();
        var restaurants = new com.dongsoop.dongsoop.search.repository.RestaurantSearchRepositoryImpl(operations);
        assertThat(restaurants.findAutocompleteSuggestionsDynamic("campus", PageRequest.of(0, 10)))
                .extracting(com.dongsoop.dongsoop.search.entity.RestaurantDocument::getRestaurantId)
                .containsExactlyInAnyOrder(101L, 102L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void restaurantAutocompleteRejectsMissingKeyword(String keyword) {
        var restaurants = new com.dongsoop.dongsoop.search.repository.RestaurantSearchRepositoryImpl(operations);
        assertThat(restaurants.findAutocompleteSuggestionsDynamic(keyword, PageRequest.of(0, 10))).isEmpty();
    }

    private void seedRestaurants() throws IOException {
        var client = new ElasticsearchClient(transport);
        for (long id : List.of(101L, 102L)) {
            String name = "campus noodles";
            if (id == 102L) {
                name = "campus coffee";
            }
            var document = java.util.Map.of("board_type", "RESTAURANT", "board_id", id, "title", name,
                    "content", "food", "tags", "campus", "link", "https://example.test/restaurant/" + id);
            client.index(i -> i.index("boards").id("restaurant-" + id).document(document));
        }
        operations.indexOps(BoardDocument.class).refresh();
    }

    private BoardDocument document(String id, String type, String marketplaceType, String department) {
        return new BoardDocument(id, "campus " + id, "content", type, Long.valueOf(id), "writer", "2026-10-14",
                null, null, "OPEN", marketplaceType, "/notice/" + id, department, 1000L, department, 0);
    }
}
