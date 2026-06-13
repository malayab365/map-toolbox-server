package com.example.mcptoolbox.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP utility tools for working with JSON (no external connection required).
 *
 * <p>Uses Jackson for parse/validate/pretty-print and JsonPath (bundled with
 * spring-data-mongodb) for querying.
 */
@Component
public class JsonTools {

    private final ObjectMapper mapper = new ObjectMapper();

    @Tool(description = "Validate whether a string is well-formed JSON. Returns 'valid' or an error message describing the problem.")
    public String jsonValidate(@ToolParam(description = "JSON text to validate.") String json) {
        try {
            mapper.readTree(json);
            return "valid";
        } catch (Exception e) {
            return "invalid: " + e.getMessage();
        }
    }

    @Tool(description = "Pretty-print (indent) a JSON string.")
    public String jsonPrettyPrint(@ToolParam(description = "JSON text to format.") String json) throws Exception {
        JsonNode node = mapper.readTree(json);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

    @Tool(description = "Minify a JSON string by removing all insignificant whitespace.")
    public String jsonMinify(@ToolParam(description = "JSON text to minify.") String json) throws Exception {
        JsonNode node = mapper.readTree(json);
        return mapper.writeValueAsString(node);
    }

    @Tool(description = "Extract a value from a JSON document using a JSONPath expression, e.g. $.store.book[0].title.")
    public String jsonPath(
            @ToolParam(description = "JSON text to query.") String json,
            @ToolParam(description = "JSONPath expression.") String path) {
        Object result = JsonPath.read(json, path);
        return String.valueOf(result);
    }
}
