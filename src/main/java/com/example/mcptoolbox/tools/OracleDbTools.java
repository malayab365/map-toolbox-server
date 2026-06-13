package com.example.mcptoolbox.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP tools for an Oracle (or any JDBC) database.
 *
 * <p>Backed by a {@link JdbcTemplate}. The DataSource is configured in
 * application.yml under {@code spring.datasource.*}. Injected lazily via
 * {@link ObjectProvider} so the server still starts when no DB is configured.
 */
@Component
public class OracleDbTools {

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public OracleDbTools(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    private JdbcTemplate jdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new IllegalStateException(
                    "No DataSource configured. Set spring.datasource.* in application.yml.");
        }
        return jdbc;
    }

    @Tool(description = "Run a read-only SQL SELECT query against the Oracle database and return the rows as a list of column->value maps.")
    public List<Map<String, Object>> oracleQuery(
            @ToolParam(description = "A single SQL SELECT statement. Do not include a trailing semicolon.") String sql,
            @ToolParam(required = false, description = "Maximum number of rows to return (default 100).") Integer maxRows) {
        String trimmed = sql.strip();
        if (!trimmed.regionMatches(true, 0, "select", 0, 6)
                && !trimmed.regionMatches(true, 0, "with", 0, 4)) {
            throw new IllegalArgumentException("oracleQuery only accepts SELECT/WITH statements. Use oracleUpdate for writes.");
        }
        int limit = maxRows == null ? 100 : maxRows;
        JdbcTemplate jdbc = jdbc();
        jdbc.setMaxRows(limit);
        return jdbc.queryForList(trimmed);
    }

    @Tool(description = "Execute a write SQL statement (INSERT, UPDATE, DELETE, DDL) against the Oracle database. Returns the number of affected rows.")
    public int oracleUpdate(
            @ToolParam(description = "A single SQL DML/DDL statement. Do not include a trailing semicolon.") String sql) {
        return jdbc().update(sql.strip());
    }

    @Tool(description = "List table names visible to the current Oracle user (from USER_TABLES).")
    public List<String> oracleListTables() {
        return jdbc().queryForList("SELECT table_name FROM user_tables ORDER BY table_name", String.class);
    }

    @Tool(description = "Describe the columns of an Oracle table: name, data type, nullability.")
    public List<Map<String, Object>> oracleDescribeTable(
            @ToolParam(description = "Table name (case-insensitive).") String tableName) {
        return jdbc().queryForList(
                "SELECT column_name, data_type, data_length, nullable " +
                        "FROM user_tab_columns WHERE table_name = UPPER(?) ORDER BY column_id",
                tableName);
    }
}
