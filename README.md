# MCP Toolbox Server

A Spring Boot ([Spring AI](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)) **MCP server** that exposes tools for connecting to common backend systems:

| Connector | Tools |
|-----------|-------|
| **Oracle / JDBC** | `oracleQuery`, `oracleUpdate`, `oracleListTables`, `oracleDescribeTable` |
| **MongoDB** | `mongoListCollections`, `mongoFind`, `mongoInsert`, `mongoCount`, `mongoDelete` |
| **Redis** | `redisGet`, `redisSet`, `redisDelete`, `redisKeys`, `redisTtl` |
| **Kafka** | `kafkaPublish`, `kafkaConsume` |
| **JSON** | `jsonValidate`, `jsonPrettyPrint`, `jsonMinify`, `jsonPath` |
| **JMS / IBM MQ** | `mqSend`, `mqReceive` |

Each connector is injected lazily, so the server starts even when a backend
is unreachable — the relevant tool simply returns a clear "not configured"
error when called.

## Requirements

- Java 21
- Maven 3.9+

## Build & run

```bash
mvn clean package
```

### SSE / HTTP transport (default)

```bash
java -jar target/mcp-toolbox-server-0.1.0.jar
```

- SSE stream:   `GET  http://localhost:8080/sse`
- Message post: `POST http://localhost:8080/mcp/message`

### STDIO transport (e.g. Claude Desktop)

```bash
java -jar target/mcp-toolbox-server-0.1.0.jar --spring.profiles.active=stdio
```

The `stdio` profile disables the web server, banner, and console logging
(stdout is reserved for the MCP protocol) and writes logs to
`./mcp-toolbox-server.log`.

#### Claude Desktop config

Add to `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "toolbox": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-toolbox-server-0.1.0.jar",
        "--spring.profiles.active=stdio"
      ],
      "env": {
        "ORACLE_URL": "jdbc:oracle:thin:@//db-host:1521/ORCLPDB1",
        "ORACLE_USER": "app",
        "ORACLE_PASSWORD": "secret",
        "MONGODB_URI": "mongodb://localhost:27017/appdb",
        "REDIS_HOST": "localhost",
        "KAFKA_BOOTSTRAP": "localhost:9092",
        "MQ_QMGR": "QM1",
        "MQ_CONN": "localhost(1414)"
      }
    }
  }
}
```

## Configuration

All connection settings live in `src/main/resources/application.yml` and are
overridable via environment variables:

| Env var | Purpose | Default |
|---------|---------|---------|
| `ORACLE_URL` / `ORACLE_USER` / `ORACLE_PASSWORD` | Oracle JDBC | `localhost:1521/XEPDB1` |
| `MONGODB_URI` | MongoDB connection string | `mongodb://localhost:27017/appdb` |
| `REDIS_HOST` / `REDIS_PORT` | Redis | `localhost:6379` |
| `KAFKA_BOOTSTRAP` | Kafka brokers | `localhost:9092` |
| `MQ_QMGR` / `MQ_CHANNEL` / `MQ_CONN` / `MQ_USER` / `MQ_PASSWORD` | IBM MQ | dev defaults |
| `SERVER_PORT` | HTTP port (SSE mode) | `8080` |

## Adding a new connector

1. Create a class in `com.example.mcptoolbox.tools` annotated with `@Component`.
2. Add public methods annotated with `@Tool(description = "...")`; annotate
   parameters with `@ToolParam`.
3. Register the bean in `McpToolboxApplication#toolboxTools(...)`.

The `@Tool` descriptions are what the LLM sees, so keep them precise.

## Project layout

```
src/main/java/com/example/mcptoolbox/
├── McpToolboxApplication.java     # entry point + tool registration
└── tools/
    ├── OracleDbTools.java
    ├── MongoTools.java
    ├── RedisTools.java
    ├── KafkaTools.java
    ├── JsonTools.java
    └── JmsMqTools.java
src/main/resources/
├── application.yml                # SSE mode + connector config
└── application-stdio.yml          # STDIO profile
```

## Safety notes

- `oracleQuery` rejects non-SELECT statements; writes go through `oracleUpdate`.
  Both run whatever SQL they're given — restrict the DB user's grants and
  consider read-only credentials in production.
- `redisKeys` uses `KEYS` which can be expensive on large datasets.
- Treat this server as privileged: anything that can reach the MCP endpoint can
  reach your backends with the configured credentials.
