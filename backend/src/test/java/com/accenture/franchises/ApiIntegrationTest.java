package com.accenture.franchises;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApiIntegrationTest {

    private static final String ADMIN_EMAIL = TestEnv.get("ADMIN_EMAIL");
    private static final String ADMIN_PASSWORD = TestEnv.get("ADMIN_PASSWORD");
    private static final String USER_PASSWORD = TestEnv.get("USER_PASSWORD");

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("franchises")
                    .withUsername("franchises")
                    .withPassword("franchises");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String r2dbc = "r2dbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432)
                + "/franchises";
        registry.add("spring.r2dbc.url", () -> r2dbc);
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("app.security.jwt-secret", () -> "test-secret-with-at-least-32-characters!!");
        registry.add("app.security.admin-email", () -> ADMIN_EMAIL);
        registry.add("app.security.admin-password", () -> ADMIN_PASSWORD);
        registry.add("app.security.cookie-secure", () -> "false");
    }

    @LocalServerPort
    int port;

    WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void csrfEndpointIssuesCookie() {
        csrfToken();
    }

    @Test
    void loginRejectsWrongPassword() {
        String csrf = csrfToken();
        client.post().uri("/api/v1/auth/login")
                .cookie("XSRF-TOKEN", csrf)
                .header("X-XSRF-TOKEN", csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", ADMIN_EMAIL, "password", "incorrecta1!"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void loginIsThrottledAfterRepeatedFailures() {
        String mail = email();
        for (int attempt = 0; attempt < 5; attempt++) {
            String csrf = csrfToken();
            client.post().uri("/api/v1/auth/login")
                    .cookie("XSRF-TOKEN", csrf)
                    .header("X-XSRF-TOKEN", csrf)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("email", mail, "password", "incorrecta1!"))
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
        String csrf = csrfToken();
        client.post().uri("/api/v1/auth/login")
                .cookie("XSRF-TOKEN", csrf)
                .header("X-XSRF-TOKEN", csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", mail, "password", "incorrecta1!"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void invalidQueryParamIsBadRequest() {
        Session admin = loginAdmin();
        client.get().uri("/api/v1/franchises?page=abc")
                .cookie("access_token", admin.access())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void loginReturnsSessionAndMeWorks() {
        Session session = loginAdmin();
        client.get().uri("/api/v1/auth/me")
                .cookie("access_token", session.access())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo(ADMIN_EMAIL)
                .jsonPath("$.roles[0]").isEqualTo("ADMIN")
                .jsonPath("$.permissions.length()").isEqualTo(5);
    }

    @Test
    void anyRoleCanEditOwnProfile() {
        Session user = loginUser();

        patch(user, "/api/v1/auth/me", Map.of("displayName", "Vista Renombrada"))
                .expectStatus().isOk()
                .expectBody().jsonPath("$.displayName").isEqualTo("Vista Renombrada");

        client.get().uri("/api/v1/auth/me").cookie("access_token", user.access()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.displayName").isEqualTo("Vista Renombrada");

        patch(user, "/api/v1/auth/me", Map.of("password", "debil1"))
                .expectStatus().isBadRequest();

        patch(user, "/api/v1/auth/me", Map.of("displayName", "Vista", "password", "Nueva123!"))
                .expectStatus().isOk();
    }

    @Test
    void profileCannotBeEditedWithoutSession() {
        String csrf = csrfToken();
        client.patch().uri("/api/v1/auth/me")
                .cookie("XSRF-TOKEN", csrf)
                .header("X-XSRF-TOKEN", csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("displayName", "Anonimo"))
                .exchange()
                .expectStatus().value(status -> {
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Se esperaba 401/403 y llego " + status);
                    }
                });
    }

    @Test
    void logoutClearsCookie() {
        Session session = loginAdmin();
        client.post().uri("/api/v1/auth/logout")
                .cookie("access_token", session.access())
                .cookie("XSRF-TOKEN", session.csrf())
                .header("X-XSRF-TOKEN", session.csrf())
                .exchange()
                .expectStatus().isNoContent()
                .expectCookie().maxAge("access_token", Duration.ZERO);
    }

    @Test
    void registerRejectsWeakPassword() {
        String csrf = csrfToken();
        client.post().uri("/api/v1/auth/register")
                .cookie("XSRF-TOKEN", csrf)
                .header("X-XSRF-TOKEN", csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email(), "password", "debil123", "displayName", "Prueba"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void registerCreatesUser() {
        String csrf = csrfToken();
        client.post().uri("/api/v1/auth/register")
                .cookie("XSRF-TOKEN", csrf)
                .header("X-XSRF-TOKEN", csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email(), "password", USER_PASSWORD, "displayName", "Prueba"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.roles[0]").isEqualTo("USER");
    }

    @Test
    void requestsWithoutSessionAreUnauthorized() {
        client.get().uri("/api/v1/users").exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/v1/franchises").exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/v2/inexistente").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void writesWithoutCsrfHeaderAreForbidden() {
        Session session = loginAdmin();
        client.post().uri("/api/v1/franchises")
                .cookie("access_token", session.access())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", name("Sin CSRF")))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void undeclaredRouteIsForbiddenWithSession() {
        Session session = loginAdmin();
        client.get().uri("/api/v2/inexistente")
                .cookie("access_token", session.access())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void healthIsPublic() {
        client.get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void userListRequiresAdmin() {
        Session admin = loginAdmin();
        client.get().uri("/api/v1/users").cookie("access_token", admin.access()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.content").isArray();

        Session user = loginUser();
        client.get().uri("/api/v1/users").cookie("access_token", user.access()).exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void adminCreatesUserAndValidatesInput() {
        Session admin = loginAdmin();
        String mail = email();
        String body = post(admin, "/api/v1/users",
                Map.of("email", mail, "password", USER_PASSWORD, "displayName", "Prueba",
                        "roles", List.of("USER")))
                .expectStatus().isCreated()
                .returnResult(String.class).getResponseBody().blockFirst();
        String id = required(field(body, "id"));

        post(admin, "/api/v1/users",
                Map.of("email", email(), "password", "debil1", "displayName", "Prueba",
                        "roles", List.of("USER")))
                .expectStatus().isBadRequest();

        post(admin, "/api/v1/users",
                Map.of("email", email(), "password", USER_PASSWORD, "displayName", "Prueba",
                        "roles", List.of("SUPERUSER")))
                .expectStatus().isBadRequest();

        client.get().uri("/api/v1/users/" + id).cookie("access_token", admin.access()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.email").isEqualTo(mail);
    }

    @Test
    void patchUserRejectsStaleVersionAndKeepsRoles() {
        Session admin = loginAdmin();
        String id = createUser(admin, List.of("USER"));

        patch(admin, "/api/v1/users/" + id, Map.of("displayName", "Otro", "version", 99))
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        patch(admin, "/api/v1/users/" + id, Map.of("displayName", "Otro", "version", 0))
                .expectStatus().isOk()
                .expectBody().jsonPath("$.displayName").isEqualTo("Otro")
                .jsonPath("$.roles[0]").isEqualTo("USER");
    }

    @Test
    void deleteUserRemovesIt() {
        Session admin = loginAdmin();
        String id = createUser(admin, List.of("USER"));
        delete(admin, "/api/v1/users/" + id).expectStatus().isNoContent();
        client.get().uri("/api/v1/users/" + id).cookie("access_token", admin.access()).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void franchiseCrud() {
        Session admin = loginAdmin();
        String franchiseName = name("Centro");
        String created = post(admin, "/api/v1/franchises", Map.of("name", franchiseName))
                .expectStatus().isCreated()
                .returnResult(String.class).getResponseBody().blockFirst();
        String id = field(created, "id");

        client.get().uri("/api/v1/franchises").cookie("access_token", admin.access()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isNumber();

        client.get().uri("/api/v1/franchises/" + id).cookie("access_token", admin.access()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo(franchiseName)
                .jsonPath("$.branchCount").isEqualTo(0);

        post(admin, "/api/v1/franchises", Map.of("name", franchiseName.toLowerCase()))
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        patch(admin, "/api/v1/franchises/" + id + "?version=0", Map.of("name", franchiseName + " II"))
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo(franchiseName + " II");

        patch(admin, "/api/v1/franchises/" + id + "?version=0", Map.of("name", "Vieja"))
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        delete(admin, "/api/v1/franchises/" + id).expectStatus().isNoContent();
        client.get().uri("/api/v1/franchises/" + id).cookie("access_token", admin.access()).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void franchiseNameIsRequired() {
        Session admin = loginAdmin();
        post(admin, "/api/v1/franchises", Map.of("name", "   "))
                .expectStatus().isBadRequest();
    }

    @Test
    void userOnlySeesAssignedFranchises() {
        Session admin = loginAdmin();
        String assigned = createFranchise(admin);
        String foreign = createFranchise(admin);
        String userId = createUser(admin, List.of("USER"));
        put(admin, "/api/v1/franchises/" + assigned + "/members",
                Map.of("userIds", List.of(userId)))
                .expectStatus().isNoContent();

        String mail = field(
                client.get().uri("/api/v1/users/" + userId).cookie("access_token", admin.access())
                        .exchange().expectStatus().isOk()
                        .returnResult(String.class).getResponseBody().blockFirst(),
                "email");
        Session user = login(mail, USER_PASSWORD);

        String list = body(client.get().uri("/api/v1/franchises?page=0&size=50")
                .cookie("access_token", user.access()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(1)
                .returnResult());
        required(list);
        if (!list.contains(assigned)) {
            throw new AssertionError("La lista no contiene la franquicia asignada: " + list);
        }

        client.get().uri("/api/v1/franchises/" + foreign).cookie("access_token", user.access())
                .exchange().expectStatus().isForbidden();
        client.get().uri("/api/v1/franchises/" + foreign + "/products")
                .cookie("access_token", user.access()).exchange().expectStatus().isForbidden();
        post(user, "/api/v1/franchises", Map.of("name", name("Prohibida")))
                .expectStatus().isForbidden();
    }

    @Test
    void branchCrud() {
        Session admin = loginAdmin();
        String fid = createFranchise(admin);
        String created = post(admin, "/api/v1/franchises/" + fid + "/branches",
                Map.of("name", "Palermo"))
                .expectStatus().isCreated()
                .returnResult(String.class).getResponseBody().blockFirst();
        String bid = field(created, "id");

        client.get().uri("/api/v1/franchises/" + fid + "/branches")
                .cookie("access_token", admin.access()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(1);

        post(admin, "/api/v1/franchises/" + fid + "/branches", Map.of("name", "palermo"))
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        patch(admin, "/api/v1/franchises/" + fid + "/branches/" + bid,
                Map.of("name", "Belgrano", "version", 0))
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo("Belgrano");

        delete(admin, "/api/v1/franchises/" + fid + "/branches/" + bid)
                .expectStatus().isNoContent();
        client.get().uri("/api/v1/franchises/" + fid + "/branches")
                .cookie("access_token", admin.access()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void productCrudWithSearchAndStock() {
        Session admin = loginAdmin();
        String fid = createFranchise(admin);
        String bid = createBranch(admin, fid, "Palermo");
        String pid = createProduct(admin, fid, bid, "Cafe Molido", 40);
        createProduct(admin, fid, bid, "Te Verde", 12);

        client.get().uri("/api/v1/franchises/" + fid + "/products?page=0&size=10")
                .cookie("access_token", admin.access()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(2)
                .jsonPath("$.content[0].branchName").isEqualTo("Palermo");

        client.get().uri("/api/v1/franchises/" + fid + "/products?search=molido")
                .cookie("access_token", admin.access()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(1)
                .jsonPath("$.content[0].name").isEqualTo("Cafe Molido");

        patch(admin, "/api/v1/franchises/" + fid + "/branches/" + bid + "/products/" + pid,
                Map.of("name", "Cafe en grano", "stock", 55, "version", 0))
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo("Cafe en grano")
                .jsonPath("$.stock").isEqualTo(55)
                .jsonPath("$.version").isEqualTo(1);

        patch(admin, "/api/v1/franchises/" + fid + "/branches/" + bid + "/products/" + pid
                        + "/stock", Map.of("stock", 70, "version", 1))
                .expectStatus().isOk()
                .expectBody().jsonPath("$.stock").isEqualTo(70);

        patch(admin, "/api/v1/franchises/" + fid + "/branches/" + bid + "/products/" + pid
                        + "/stock", Map.of("stock", 10, "version", 1))
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        patch(admin, "/api/v1/franchises/" + fid + "/branches/" + bid + "/products/" + pid
                        + "/stock", Map.of("stock", -1, "version", 2))
                .expectStatus().isBadRequest();

        delete(admin, "/api/v1/franchises/" + fid + "/branches/" + bid + "/products/" + pid)
                .expectStatus().isNoContent();
        client.get().uri("/api/v1/franchises/" + fid + "/products")
                .cookie("access_token", admin.access()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(1);
    }

    @Test
    void productRequiresExistingBranchOfTheSameFranchise() {
        Session admin = loginAdmin();
        String fid = createFranchise(admin);
        String other = createFranchise(admin);
        String bid = createBranch(admin, other, "Ajena");
        post(admin, "/api/v1/franchises/" + fid + "/branches/" + bid + "/products",
                Map.of("name", "Cruzado", "stock", 1))
                .expectStatus().isNotFound();
    }

    @Test
    void branchesOfMissingFranchiseAreNotFound() {
        Session admin = loginAdmin();
        client.get().uri("/api/v1/franchises/" + UUID.randomUUID() + "/branches")
                .cookie("access_token", admin.access()).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void productsOfMissingFranchiseAreNotFound() {
        Session admin = loginAdmin();
        client.get().uri("/api/v1/franchises/" + UUID.randomUUID() + "/products")
                .cookie("access_token", admin.access()).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void registerAssignsOneFranchise() {
        Session admin = loginAdmin();
        createFranchise(admin);
        String csrf = csrfToken();
        List<String> cookies = client.post().uri("/api/v1/auth/register")
                .cookie("XSRF-TOKEN", csrf)
                .header("X-XSRF-TOKEN", csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email(), "password", USER_PASSWORD,
                        "displayName", "Nuevo"))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(String.class).getResponseHeaders().get(HttpHeaders.SET_COOKIE);

        client.get().uri("/api/v1/franchises?page=0&size=10")
                .cookie("access_token", cookie(cookies, "access_token")).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.content[0].id").exists();
    }

    private record Session(String access, String csrf) {
    }

    private String csrfToken() {
        List<String> cookies = client.get().uri("/api/v1/auth/csrf").exchange()
                .expectStatus().isNoContent()
                .returnResult(Void.class).getResponseHeaders().get(HttpHeaders.SET_COOKIE);
        return cookie(cookies, "XSRF-TOKEN");
    }

    private Session loginAdmin() {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private Session loginUser() {
        String mail = email();
        createUser(loginAdmin(), List.of("USER"), mail);
        return login(mail, USER_PASSWORD);
    }

    private Session login(String email, String password) {
        String csrf = csrfToken();
        List<String> cookies = client.post().uri("/api/v1/auth/login")
                .cookie("XSRF-TOKEN", csrf)
                .header("X-XSRF-TOKEN", csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", password))
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class).getResponseHeaders().get(HttpHeaders.SET_COOKIE);
        return new Session(cookie(cookies, "access_token"), csrf);
    }

    private String createUser(Session admin, List<String> roles) {
        return createUser(admin, roles, email());
    }

    private String createUser(Session admin, List<String> roles, String mail) {
        String body = post(admin, "/api/v1/users",
                Map.of("email", mail, "password", USER_PASSWORD, "displayName", "Prueba", "roles", roles))
                .expectStatus().isCreated()
                .returnResult(String.class).getResponseBody().blockFirst();
        return field(body, "id");
    }

    private String createFranchise(Session admin) {
        String body = post(admin, "/api/v1/franchises", Map.of("name", name("Red")))
                .expectStatus().isCreated()
                .returnResult(String.class).getResponseBody().blockFirst();
        return field(body, "id");
    }

    private String createBranch(Session admin, String franchiseId, String branchName) {
        String body = post(admin, "/api/v1/franchises/" + franchiseId + "/branches",
                Map.of("name", branchName))
                .expectStatus().isCreated()
                .returnResult(String.class).getResponseBody().blockFirst();
        return field(body, "id");
    }

    private String createProduct(Session admin, String franchiseId, String branchId,
                                 String productName, int stock) {
        String body = post(admin,
                "/api/v1/franchises/" + franchiseId + "/branches/" + branchId + "/products",
                Map.of("name", productName, "stock", stock))
                .expectStatus().isCreated()
                .returnResult(String.class).getResponseBody().blockFirst();
        return field(body, "id");
    }

    private WebTestClient.ResponseSpec post(Session session, String uri, Object body) {
        return client.post().uri(uri)
                .cookie("access_token", session.access())
                .cookie("XSRF-TOKEN", session.csrf())
                .header("X-XSRF-TOKEN", session.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    private WebTestClient.ResponseSpec patch(Session session, String uri, Object body) {
        return client.patch().uri(uri)
                .cookie("access_token", session.access())
                .cookie("XSRF-TOKEN", session.csrf())
                .header("X-XSRF-TOKEN", session.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    private WebTestClient.ResponseSpec put(Session session, String uri, Object body) {
        return client.put().uri(uri)
                .cookie("access_token", session.access())
                .cookie("XSRF-TOKEN", session.csrf())
                .header("X-XSRF-TOKEN", session.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    private WebTestClient.ResponseSpec delete(Session session, String uri) {
        return client.delete().uri(uri)
                .cookie("access_token", session.access())
                .cookie("XSRF-TOKEN", session.csrf())
                .header("X-XSRF-TOKEN", session.csrf())
                .exchange();
    }

    private static String cookie(List<String> setCookies, String name) {
        return setCookies.stream()
                .filter(value -> value.startsWith(name + "="))
                .map(value -> value.substring(name.length() + 1).split(";")[0])
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cookie ausente: " + name));
    }

    private static String field(String json, String name) {
        if (json == null) {
            throw new IllegalStateException("Respuesta vacia al buscar " + name);
        }
        Matcher matcher = Pattern.compile("\"" + name + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Campo ausente: " + name + " en " + json);
        }
        return matcher.group(1);
    }

    private static String body(org.springframework.test.web.reactive.server.EntityExchangeResult<byte[]> result) {
        return new String(result.getResponseBody(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static <T> T required(T value) {
        if (value == null) {
            throw new AssertionError("Respuesta vacia");
        }
        return value;
    }

    private static String email() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@acento.com";
    }

    private static String name(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }
}
