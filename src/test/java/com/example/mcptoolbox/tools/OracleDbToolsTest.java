package com.example.mcptoolbox.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OracleDbToolsTest {

    @Mock ObjectProvider<JdbcTemplate> provider;
    @Mock JdbcTemplate jdbc;

    private OracleDbTools tools;

    @BeforeEach
    void setUp() {
        tools = new OracleDbTools(provider);
    }

    // --- jdbc() throws when no DataSource ---

    @Test
    void oracleQuery_noDataSource_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.oracleQuery("SELECT 1", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No DataSource configured");
    }

    // --- oracleQuery ---

    @Test
    void oracleQuery_selectStatement_delegatesToJdbc() {
        when(provider.getIfAvailable()).thenReturn(jdbc);
        List<Map<String, Object>> rows = List.of(Map.of("ID", 1));
        when(jdbc.queryForList(anyString())).thenReturn(rows);

        List<Map<String, Object>> result = tools.oracleQuery("SELECT * FROM emp", null);

        verify(jdbc).setMaxRows(100);
        assertThat(result).isEqualTo(rows);
    }

    @Test
    void oracleQuery_withMaxRows_setsLimit() {
        when(provider.getIfAvailable()).thenReturn(jdbc);
        when(jdbc.queryForList(anyString())).thenReturn(List.of());

        tools.oracleQuery("SELECT 1 FROM dual", 5);

        verify(jdbc).setMaxRows(5);
    }

    @Test
    void oracleQuery_withCTEStatement_isAllowed() {
        when(provider.getIfAvailable()).thenReturn(jdbc);
        when(jdbc.queryForList(anyString())).thenReturn(List.of());

        assertThatCode(() -> tools.oracleQuery("WITH t AS (SELECT 1) SELECT * FROM t", null))
                .doesNotThrowAnyException();
    }

    @Test
    void oracleQuery_insertStatement_throwsIllegalArgument() {
        // Guard fires before jdbc() is called, so no provider stub needed
        assertThatThrownBy(() -> tools.oracleQuery("INSERT INTO t VALUES(1)", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oracleQuery only accepts SELECT/WITH");
    }

    @Test
    void oracleQuery_caseInsensitiveSelect_isAllowed() {
        when(provider.getIfAvailable()).thenReturn(jdbc);
        when(jdbc.queryForList(anyString())).thenReturn(List.of());

        assertThatCode(() -> tools.oracleQuery("select id from emp", null))
                .doesNotThrowAnyException();
    }

    // --- oracleUpdate ---

    @Test
    void oracleUpdate_delegatesToJdbc() {
        when(provider.getIfAvailable()).thenReturn(jdbc);
        when(jdbc.update(anyString())).thenReturn(3);

        int affected = tools.oracleUpdate("DELETE FROM tmp");

        assertThat(affected).isEqualTo(3);
        verify(jdbc).update("DELETE FROM tmp");
    }

    @Test
    void oracleUpdate_stripsWhitespace() {
        when(provider.getIfAvailable()).thenReturn(jdbc);
        when(jdbc.update("UPDATE t SET x=1")).thenReturn(1);

        tools.oracleUpdate("  UPDATE t SET x=1  ");

        verify(jdbc).update("UPDATE t SET x=1");
    }

    @Test
    void oracleUpdate_noDataSource_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.oracleUpdate("DELETE FROM t"))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- oracleListTables ---

    @Test
    void oracleListTables_returnsList() {
        when(provider.getIfAvailable()).thenReturn(jdbc);
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(List.of("EMP", "DEPT"));

        List<String> tables = tools.oracleListTables();

        assertThat(tables).containsExactly("EMP", "DEPT");
    }

    @Test
    void oracleListTables_noDataSource_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(tools::oracleListTables)
                .isInstanceOf(IllegalStateException.class);
    }

    // --- oracleDescribeTable ---

    @Test
    void oracleDescribeTable_returnsColumns() {
        when(provider.getIfAvailable()).thenReturn(jdbc);
        List<Map<String, Object>> cols = List.of(Map.of("COLUMN_NAME", "ID", "DATA_TYPE", "NUMBER"));
        when(jdbc.queryForList(anyString(), eq("EMP"))).thenReturn(cols);

        List<Map<String, Object>> result = tools.oracleDescribeTable("EMP");

        assertThat(result).isEqualTo(cols);
    }

    @Test
    void oracleDescribeTable_noDataSource_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.oracleDescribeTable("EMP"))
                .isInstanceOf(IllegalStateException.class);
    }
}
