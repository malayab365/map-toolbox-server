package com.example.mcptoolbox.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JsonToolsTest {

    private JsonTools jsonTools;

    @BeforeEach
    void setUp() {
        jsonTools = new JsonTools();
    }

    // --- jsonValidate ---

    @Test
    void jsonValidate_validJson_returnsValid() {
        assertThat(jsonTools.jsonValidate("{\"key\":\"value\"}")).isEqualTo("valid");
    }

    @Test
    void jsonValidate_validArray_returnsValid() {
        assertThat(jsonTools.jsonValidate("[1,2,3]")).isEqualTo("valid");
    }

    @Test
    void jsonValidate_invalidJson_returnsErrorMessage() {
        String result = jsonTools.jsonValidate("{invalid}");
        assertThat(result).startsWith("invalid:");
    }

    @Test
    void jsonValidate_emptyString_returnsValid() {
        // Jackson treats empty string as a valid null/missing node
        String result = jsonTools.jsonValidate("");
        assertThat(result).isEqualTo("valid");
    }

    // --- jsonPrettyPrint ---

    @Test
    void jsonPrettyPrint_compactJson_returnsIndented() throws Exception {
        String result = jsonTools.jsonPrettyPrint("{\"a\":1}");
        assertThat(result).contains("\n");
        assertThat(result).contains("\"a\" : 1");
    }

    @Test
    void jsonPrettyPrint_alreadyPretty_roundTrips() throws Exception {
        String input = "{ \"x\": [1, 2, 3] }";
        String result = jsonTools.jsonPrettyPrint(input);
        assertThat(result).contains("\"x\"");
    }

    @Test
    void jsonPrettyPrint_invalidJson_throwsException() {
        assertThatThrownBy(() -> jsonTools.jsonPrettyPrint("{bad}"))
                .isInstanceOf(Exception.class);
    }

    // --- jsonMinify ---

    @Test
    void jsonMinify_prettifiedJson_removesWhitespace() throws Exception {
        String result = jsonTools.jsonMinify("{\n  \"key\" : \"value\"\n}");
        assertThat(result).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void jsonMinify_alreadyMinified_unchanged() throws Exception {
        String input = "{\"a\":1}";
        assertThat(jsonTools.jsonMinify(input)).isEqualTo(input);
    }

    @Test
    void jsonMinify_invalidJson_throwsException() {
        assertThatThrownBy(() -> jsonTools.jsonMinify("not json"))
                .isInstanceOf(Exception.class);
    }

    // --- jsonPath ---

    @Test
    void jsonPath_simpleField_returnsValue() {
        String result = jsonTools.jsonPath("{\"name\":\"Alice\"}", "$.name");
        assertThat(result).isEqualTo("Alice");
    }

    @Test
    void jsonPath_nestedField_returnsValue() {
        String result = jsonTools.jsonPath("{\"a\":{\"b\":42}}", "$.a.b");
        assertThat(result).isEqualTo("42");
    }

    @Test
    void jsonPath_arrayIndex_returnsElement() {
        String result = jsonTools.jsonPath("[\"x\",\"y\",\"z\"]", "$[1]");
        assertThat(result).isEqualTo("y");
    }

    @Test
    void jsonPath_missingPath_throwsException() {
        assertThatThrownBy(() -> jsonTools.jsonPath("{\"a\":1}", "$.missing"))
                .isInstanceOf(Exception.class);
    }
}
