package ru.practicum.shareit.client;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BaseClientTest {

    private static final String SERVER = "http://server";

    private MockRestServiceServer server;
    private TestClient client;

    static class TestClient extends BaseClient {
        TestClient(RestTemplate rest) {
            super(rest);
        }

        ResponseEntity<Object> doGet(String path, Long userId) {
            return get(path, userId, null);
        }

        ResponseEntity<Object> doDelete(String path) {
            return delete(path);
        }
    }

    @BeforeEach
    void setUp() {
        RestTemplate rest = new RestTemplate();
        rest.setUriTemplateHandler(new DefaultUriBuilderFactory(SERVER));
        server = MockRestServiceServer.bindTo(rest).build();
        client = new TestClient(rest);
    }

    @Test
    void errorFromServerIsPassedAsJson() {
        server.expect(requestTo(SERVER + "/users/1"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"User with id=1 not found\"}"));

        ResponseEntity<Object> response = client.doGet("/users/1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isInstanceOf(String.class);
        assertThat((String) response.getBody()).contains("User with id=1 not found");
    }

    @Test
    void noContentPassesWithoutBody() {
        server.expect(requestTo(SERVER + "/users/1")).andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        ResponseEntity<Object> response = client.doDelete("/users/1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.hasBody()).isFalse();
    }

    @Test
    void userIdIsSentAsHeader() {
        server.expect(requestTo(SERVER + "/items")).andExpect(header("X-Sharer-User-Id", "7"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.doGet("/items", 7L).getStatusCode()).isEqualTo(HttpStatus.OK);
        server.verify();
    }

    @Test
    void nullUserIdSendsNoHeader() {
        server.expect(requestTo(SERVER + "/items/5")).andExpect(headerDoesNotExist("X-Sharer-User-Id"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(client.doGet("/items/5", null).getStatusCode()).isEqualTo(HttpStatus.OK);
        server.verify();
    }

    @Test
    void successBodyIsReturned() {
        server.expect(requestTo(SERVER + "/users/1"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        ResponseEntity<Object> response = client.doGet("/users/1", null);

        assertThat(response.getBody()).isEqualTo(Map.of("id", 1));
    }
}
