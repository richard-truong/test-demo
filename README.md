# test-demo — CRUD users with hexagonal architecture

A deliberately small project that shows two things:

1. how to organise a CRUD feature with the **hexagonal (ports & adapters)** pattern
2. the difference between a **unit test** and an **integration test**

Stack: Java 21, Spring Boot 4.1.1, Maven, JUnit 5, Mockito, AssertJ.

## Run it

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvnw test          # run all tests
./mvnw spring-boot:run
```

## The hexagon

```
        HTTP                          the application                    storage
 ┌──────────────────┐            ┌─────────────────────┐           ┌──────────────────────┐
 │  UserController  │──uses────▶ │  UserUseCase (in)   │           │                      │
 │  ApiException-   │            │        ▲            │           │                      │
 │  Handler         │            │        │            │           │                      │
 └──────────────────┘            │   UserService       │           │                      │
                                 │        │            │           │                      │
        adapter/web              │        ▼            │           │  adapter/persistence │
        (INPUT adapter)          │ UserRepositoryPort  │──uses────▶│ InMemoryUser-        │
                                 │      (out port)     │           │ Repository           │
                                 └─────────────────────┘           └──────────────────────┘
                                    application/                      (OUTPUT adapter)
                                                                  ┌──────────────────────┐
                                 domain/User                      │  a future JPA/H2     │
                                 (pure business object)           │  adapter just drops  │
                                                                  │  in here             │
                                                                  └──────────────────────┘
```

```
src/main/java/com/example/testdemo/
├── domain/                          the core: plain Java, no framework
│   ├── User.java                    record(id, name, email)
│   └── UserNotFoundException.java
├── application/                     what the app can do + what it needs
│   ├── UserUseCase.java             INPUT port  (driving side)
│   ├── UserRepositoryPort.java      OUTPUT port (driven side)
│   └── UserService.java             the business logic, implements UserUseCase
└── adapter/                         the edges — framework lives only here
    ├── web/
    │   ├── UserController.java      INPUT adapter  → HTTP
    │   ├── UserRequest.java         request body
    │   └── ApiExceptionHandler.java domain exception → 404
    └── persistence/
        └── InMemoryUserRepository.java   OUTPUT adapter → ConcurrentHashMap
```

The rule that makes it hexagonal: **`domain/` and `application/` never import
anything from `adapter/`.** Dependencies point inwards only. `UserService` talks
to an interface it owns (`UserRepositoryPort`); the HashMap behind it is an
implementation detail.

### REST API

| Method | Path          | Success         |
|--------|---------------|-----------------|
| POST   | `/users`      | 201 + user      |
| GET    | `/users`      | 200 + list      |
| GET    | `/users/{id}` | 200 + user      |
| PUT    | `/users/{id}` | 200 + user      |
| DELETE | `/users/{id}` | 204             |

A missing user returns `404` with `{"error": "User not found: 42"}`.

```bash
curl -X POST localhost:8080/users -H 'Content-Type: application/json' \
     -d '{"name":"Alice","email":"alice@example.com"}'
```

## The two tests

### 1. Unit test — `application/UserServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)   // no Spring, no server, no database
class UserServiceTest {
    @Mock UserRepositoryPort repository;   // the port is faked
    @InjectMocks UserService service;      // built by hand, constructor injection
}
```

Tests **one class in isolation**. The repository is a Mockito mock, so nothing
outside `UserService` can make the test fail. Runs in milliseconds, and when it
breaks you know exactly which method is wrong.

Good for: business rules, edge cases, "throws when the user does not exist".

### 2. Integration test — `adapter/web/UserControllerIntegrationTest.java`

```java
@SpringBootTest                    // boots the real application context
@AutoConfigureMockMvc              // gives us a MockMvc to fire HTTP at it
class UserControllerIntegrationTest {
    @Autowired MockMvc mockMvc;
}
```

**Nothing is mocked.** A real request goes through the real controller, the real
service and the real in-memory repository — the entire hexagon. It verifies the
parts that only exist when the pieces are wired together: the URL mapping, JSON
serialisation, the `@RestControllerAdvice` turning an exception into a 404, and
the bean wiring itself.

Good for: "does `POST /users` actually return 201 with an id?".

### Which one do I write?

| | Unit test | Integration test |
|---|---|---|
| Scope | one class | the whole request path |
| Speed | milliseconds | seconds (boots Spring) |
| Fails when | logic is wrong | logic, wiring, mapping, config is wrong |
| Count | many | a few, for the important flows |

Write many unit tests for the logic, and a handful of integration tests to prove
the wiring. You do not need an integration test for every branch — the unit test
already covers those, and duplicating them just makes the suite slow.

## Adding a real database

Create `adapter/persistence/JpaUserRepository.java` implementing
`UserRepositoryPort`, annotate it `@Repository`, and delete the `@Repository`
from `InMemoryUserRepository`. No change is required in `domain/`,
`application/` or `adapter/web/` — and `UserServiceTest` keeps passing untouched,
because it only ever depended on the port interface.

## Spring Boot 4 note

`@AutoConfigureMockMvc` moved to a new module. If you get
"cannot find symbol", add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

Its package is now
`org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`
(previously `...boot.test.autoconfigure.web.servlet`).

## MCP server (Spring AI)

An MCP server exposes the user API to LLM clients as callable tools. It is a
**wrapper**: each tool makes a real HTTP request back to this application's own
`/users` endpoints, so there is exactly one implementation of the CRUD logic.

```
                         ┌──────────────────────────────┐
   MCP client            │  Spring Boot app  :8080      │
   (Claude Code,         │                              │
    Claude Desktop,      │   POST /mcp  ──▶ McpConfig   │  publishes the tools
    MCP Inspector) ──────┼──▶ tools ──▶ UserMcpTools     │
        JSON-RPC          │              │               │
        over HTTP         │              ▼               │
                          │         UserApiClient        │
                          │              │  HTTP loopback │
                          │              ▼               │
                          │   /users ──▶ UserController  │
                          │              │               │
                          │              ▼               │
                          │   UserUseCase ──▶ repository │
                          └──────────────────────────────┘
```

### The tools

| Tool | Arguments | Returns |
|---|---|---|
| `create_user` | `name`, `email` | created user as JSON |
| `get_user` | `id` | user as JSON, or the 404 message |
| `list_users` | — | JSON array of all users |
| `update_user` | `id`, `name`, `email` | updated user as JSON |
| `delete_user` | `id` | confirmation text |

Tools return plain JSON text, and HTTP errors are caught and handed back as a
readable message (`Request failed with status 404: {"error":"User not found: 42"}`)
instead of an exception, so the model can see what went wrong and react.

### Files

```
adapter/mcp/
├── UserMcpTools.java   @Tool methods — one per CRUD operation
├── UserApiClient.java  RestClient wrapper around /users
└── McpConfig.java      ToolCallbackProvider bean that publishes the tools
```

### Run and connect

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvnw spring-boot:run           # REST on :8080 and MCP on :8080/mcp
```

The tools call the app over HTTP, so the app must be running for tool calls to
work — that is the point of this design.

Connect a client:

```bash
claude mcp add --transport http user-api http://localhost:8080/mcp
```

Check it by hand:

```bash
# 1. handshake, note the Mcp-Session-Id response header
curl -sD - -X POST localhost:8080/mcp -H 'Content-Type: application/json' \
     -H 'Accept: application/json, text/event-stream' \
     -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"curl","version":"1"}}}'

# 2. list the tools (reuse the session id)
curl -s -X POST localhost:8080/mcp -H 'Content-Type: application/json' \
     -H 'Accept: application/json, text/event-stream' \
     -H 'Mcp-Session-Id: <id>' \
     -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'
```

Responses come back as `text/event-stream`, so read the `data:` line.

### Configuration

```properties
spring.ai.mcp.server.name=user-mcp-server
spring.ai.mcp.server.protocol=STREAMABLE   # STREAMABLE (/mcp) or SSE (/sse)
app.user-api.base-url=http://localhost:8080
```

The base URL is a property, so pointing the MCP server at a different instance
(a deployed one, say) needs no code change.

### Why a wrapper instead of calling the use case directly

Calling `UserUseCase` in-process would be fewer moving parts and is the more
usual hexagonal answer — the MCP adapter would then sit beside `adapter/web`
instead of in front of it. Wrapping the REST API was chosen here so the MCP
server exercises exactly the public contract clients already use. The trade-off
is a network hop and the requirement that the server be up.

## Test reports & coverage

JaCoCo is wired into the `test` phase, so the coverage report is produced by the
normal test run — no extra command needed.

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

./mvnw test                          # runs tests AND writes the coverage report
./mvnw surefire-report:report-only   # HTML report of test results
```

| Report | File |
|---|---|
| Coverage (HTML, click through) | `target/site/jacoco/index.html` |
| Coverage (machine-readable) | `target/site/jacoco/jacoco.csv` |
| Coverage (for CI tools) | `target/site/jacoco/jacoco.xml` |
| Test results (HTML) | `target/site/surefire.html` |
| Test results (raw XML) | `target/surefire-reports/*.xml` |

Open the HTML from WSL:

```bash
explorer.exe target/site/jacoco/index.html
```

or in IntelliJ, right-click the file → *Open In → Browser*.

### Current numbers

```
CLASS                       PACKAGE                 INSTR  BRANCH   LINE
UserController              adapter.web            100.0%     n/a 100.0%
ApiExceptionHandler         adapter.web            100.0%     n/a 100.0%
InMemoryUserRepository      adapter.persistence    100.0%  100.0% 100.0%
UserService                 application            100.0%     n/a 100.0%
User                        domain                 100.0%     n/a 100.0%
UserNotFoundException       domain                 100.0%     n/a 100.0%
UserMcpTools                adapter.mcp              7.6%     n/a  21.4%   <-- no tests
UserApiClient               adapter.mcp              9.2%     n/a  10.7%   <-- no tests
TestDemoApplication         (root)                  37.5%     n/a  33.3%
-----------------------------------------------------------------------------
TOTAL                                               56.7%  100.0%  55.8%
```

The core of the hexagon is fully covered. The MCP adapter drags the total down
because you asked for no tests there — that is the report doing its job, not a
problem with the build.

### Excluding classes from the number

Some classes are not worth covering. To leave them out of the totals:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>${jacoco.version}</version>
    <configuration>
        <excludes>
            <exclude>com/example/testdemo/TestDemoApplication.class</exclude>
            <exclude>com/example/testdemo/adapter/mcp/**</exclude>
        </excludes>
    </configuration>
    ...
```

With the MCP adapter and the bootstrap class excluded the total reports as 100%.

### Failing the build on low coverage

Optional, and worth adding only once you have agreed a target:

```xml
<execution>
    <id>jacoco-check</id>
    <goals><goal>check</goal></goals>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

I have left this off — with the MCP adapter untested it would fail the build
immediately. Add it after deciding what deserves tests.

### In IntelliJ

*Run with Coverage* (the shield icon next to Run) shows the same numbers inline
in the editor, per line. It uses IntelliJ's own engine, so it can differ slightly
from JaCoCo at the margins; use JaCoCo as the reference since that is what CI
will read.

## Performance tests (k6)

Load tests for the same REST API live in [`k6/`](k6/README.md) — smoke, load,
stress and spike scenarios, with thresholds that fail the run when breached.

```bash
WSL_IP=$(hostname -I | awk '{print $1}')
docker run --rm -v "$PWD/k6:/scripts" -w /scripts \
  -e BASE_URL="http://$WSL_IP:8080" grafana/k6 run smoke.js
```

See [`k6/README.md`](k6/README.md) for why the Docker run needs the WSL IP
rather than `localhost`, and what each script measures.
