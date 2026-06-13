package com.example.mcptoolbox.tools;

import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MongoToolsTest {

    @Mock ObjectProvider<MongoTemplate> provider;
    @Mock MongoTemplate mongo;

    private MongoTools tools;

    @BeforeEach
    void setUp() {
        tools = new MongoTools(provider);
    }

    // --- mongo() throws when not configured ---

    @Test
    void mongoListCollections_noMongo_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(tools::mongoListCollections)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MongoDB not configured");
    }

    // --- mongoListCollections ---

    @Test
    void mongoListCollections_returnsNames() {
        when(provider.getIfAvailable()).thenReturn(mongo);
        when(mongo.getCollectionNames()).thenReturn(Set.of("orders", "users"));

        Set<String> result = tools.mongoListCollections();

        assertThat(result).containsExactlyInAnyOrder("orders", "users");
    }

    // --- mongoFind ---

    @Test
    void mongoFind_withFilter_passesQueryAndCollection() {
        when(provider.getIfAvailable()).thenReturn(mongo);
        List<Document> docs = List.of(new Document("_id", "1"));
        when(mongo.find(any(Query.class), eq(Document.class), eq("col"))).thenReturn(docs);

        List<Document> result = tools.mongoFind("col", "{\"a\":1}", 5);

        assertThat(result).isEqualTo(docs);
    }

    @Test
    void mongoFind_nullFilter_usesEmptyFilter() {
        when(provider.getIfAvailable()).thenReturn(mongo);
        when(mongo.find(any(Query.class), eq(Document.class), eq("col"))).thenReturn(List.of());

        tools.mongoFind("col", null, null);

        verify(mongo).find(any(Query.class), eq(Document.class), eq("col"));
    }

    @Test
    void mongoFind_blankFilter_usesEmptyFilter() {
        when(provider.getIfAvailable()).thenReturn(mongo);
        when(mongo.find(any(Query.class), eq(Document.class), eq("col"))).thenReturn(List.of());

        tools.mongoFind("col", "   ", null);

        verify(mongo).find(any(Query.class), eq(Document.class), eq("col"));
    }

    @Test
    void mongoFind_noMongo_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.mongoFind("col", null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- mongoInsert ---

    @Test
    void mongoInsert_parsesAndInsertsDocument() {
        when(provider.getIfAvailable()).thenReturn(mongo);
        Document inserted = new Document("name", "Bob").append("_id", "123");
        when(mongo.insert(any(Document.class), eq("col"))).thenReturn(inserted);

        Document result = tools.mongoInsert("col", "{\"name\":\"Bob\"}");

        assertThat(result.getString("name")).isEqualTo("Bob");
    }

    @Test
    void mongoInsert_noMongo_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.mongoInsert("col", "{\"x\":1}"))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- mongoCount ---

    @Test
    void mongoCount_withFilter_returnsCount() {
        when(provider.getIfAvailable()).thenReturn(mongo);
        when(mongo.count(any(Query.class), eq("col"))).thenReturn(42L);

        long result = tools.mongoCount("col", "{\"active\":true}");

        assertThat(result).isEqualTo(42L);
    }

    @Test
    void mongoCount_nullFilter_usesEmptyFilter() {
        when(provider.getIfAvailable()).thenReturn(mongo);
        when(mongo.count(any(Query.class), eq("col"))).thenReturn(0L);

        tools.mongoCount("col", null);

        verify(mongo).count(any(Query.class), eq("col"));
    }

    @Test
    void mongoCount_blankFilter_usesEmptyFilter() {
        when(provider.getIfAvailable()).thenReturn(mongo);
        when(mongo.count(any(Query.class), eq("col"))).thenReturn(0L);

        tools.mongoCount("col", "  ");

        verify(mongo).count(any(Query.class), eq("col"));
    }

    @Test
    void mongoCount_noMongo_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.mongoCount("col", null))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- mongoDelete ---

    @Test
    void mongoDelete_returnsDeletedCount() {
        when(provider.getIfAvailable()).thenReturn(mongo);
        DeleteResult deleteResult = mock(DeleteResult.class);
        when(deleteResult.getDeletedCount()).thenReturn(3L);
        when(mongo.remove(any(Query.class), eq("col"))).thenReturn(deleteResult);

        long result = tools.mongoDelete("col", "{\"status\":\"INACTIVE\"}");

        assertThat(result).isEqualTo(3L);
    }

    @Test
    void mongoDelete_noMongo_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.mongoDelete("col", "{\"x\":1}"))
                .isInstanceOf(IllegalStateException.class);
    }
}
