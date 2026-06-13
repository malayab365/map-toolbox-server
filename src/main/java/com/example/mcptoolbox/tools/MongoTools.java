package com.example.mcptoolbox.tools;

import org.bson.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * MCP tools for MongoDB, backed by {@link MongoTemplate}.
 *
 * <p>Connection configured under {@code spring.data.mongodb.*}. Queries and
 * documents are passed as raw JSON strings so the LLM can express any Mongo
 * filter.
 */
@Component
public class MongoTools {

    private final ObjectProvider<MongoTemplate> mongoProvider;

    public MongoTools(ObjectProvider<MongoTemplate> mongoProvider) {
        this.mongoProvider = mongoProvider;
    }

    private MongoTemplate mongo() {
        MongoTemplate t = mongoProvider.getIfAvailable();
        if (t == null) {
            throw new IllegalStateException(
                    "MongoDB not configured. Set spring.data.mongodb.uri in application.yml.");
        }
        return t;
    }

    @Tool(description = "List all collection names in the connected MongoDB database.")
    public Set<String> mongoListCollections() {
        return mongo().getCollectionNames();
    }

    @Tool(description = "Find documents in a MongoDB collection using a JSON filter. Returns matching documents as JSON.")
    public List<Document> mongoFind(
            @ToolParam(description = "Collection name.") String collection,
            @ToolParam(required = false, description = "MongoDB filter as a JSON string, e.g. {\"status\":\"ACTIVE\"}. Empty/null matches all.") String filterJson,
            @ToolParam(required = false, description = "Maximum number of documents to return (default 50).") Integer limit) {
        String f = (filterJson == null || filterJson.isBlank()) ? "{}" : filterJson;
        BasicQuery query = new BasicQuery(f);
        query.limit(limit == null ? 50 : limit);
        return mongo().find(query, Document.class, collection);
    }

    @Tool(description = "Insert a single JSON document into a MongoDB collection. Returns the inserted document including its generated _id.")
    public Document mongoInsert(
            @ToolParam(description = "Collection name.") String collection,
            @ToolParam(description = "Document to insert, as a JSON string.") String documentJson) {
        return mongo().insert(Document.parse(documentJson), collection);
    }

    @Tool(description = "Count documents in a MongoDB collection matching an optional JSON filter.")
    public long mongoCount(
            @ToolParam(description = "Collection name.") String collection,
            @ToolParam(required = false, description = "MongoDB filter as a JSON string. Empty/null counts all.") String filterJson) {
        String f = (filterJson == null || filterJson.isBlank()) ? "{}" : filterJson;
        return mongo().count(new BasicQuery(f), collection);
    }

    @Tool(description = "Delete documents from a MongoDB collection matching a JSON filter. Returns the number deleted.")
    public long mongoDelete(
            @ToolParam(description = "Collection name.") String collection,
            @ToolParam(description = "MongoDB filter as a JSON string identifying documents to delete.") String filterJson) {
        return mongo().remove(new BasicQuery(filterJson), collection).getDeletedCount();
    }
}
