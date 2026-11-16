package ru.practicum.shareit.item;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ItemClientTest {

    @Test
    void searchEncodesText() {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        ItemClient client = new ItemClient("http://server", new RestTemplateBuilder(customizer));
        MockRestServiceServer server = customizer.getServer();
        String text = "дрель 100% a&b";
        server.expect(request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/items/search");
            String rawQuery = request.getURI().getRawQuery();
            assertThat(rawQuery).startsWith("text=");
            assertThat(URLDecoder.decode(rawQuery.substring("text=".length()), StandardCharsets.UTF_8))
                    .isEqualTo(text);
        }).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.search(text);

        server.verify();
    }
}
