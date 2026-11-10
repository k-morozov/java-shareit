package ru.practicum.shareit.item;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ItemClient client;

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.header(USER_ID_HEADER, 7).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private void expectBadRequest(MockHttpServletRequestBuilder request) throws Exception {
        mvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(client);
    }

    @Test
    void createValidDelegates() throws Exception {
        when(client.create(eq(7L), any())).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", 1)));

        mvc.perform(json(post("/items"),
                        "{\"name\":\"Дрель\",\"description\":\"Ударная\",\"available\":true,\"requestId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(client).create(eq(7L), argThat(i -> Long.valueOf(3).equals(i.getRequestId())
                && "Дрель".equals(i.getName())));
    }

    @Test
    void createWithoutRequestIdDelegates() throws Exception {
        when(client.create(eq(7L), any())).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", 1)));

        mvc.perform(json(post("/items"), "{\"name\":\"Дрель\",\"description\":\"Ударная\",\"available\":true}"))
                .andExpect(status().isCreated());

        verify(client).create(eq(7L), argThat(i -> i.getRequestId() == null));
    }

    @Test
    void createWithoutNameIs400() throws Exception {
        expectBadRequest(json(post("/items"), "{\"description\":\"Ударная\",\"available\":true,\"requestId\":3}"));
    }

    @Test
    void createWithoutDescriptionIs400() throws Exception {
        expectBadRequest(json(post("/items"), "{\"name\":\"Дрель\",\"available\":true,\"requestId\":3}"));
    }

    @Test
    void createWithoutAvailableIs400() throws Exception {
        expectBadRequest(json(post("/items"), "{\"name\":\"Дрель\",\"description\":\"Ударная\",\"requestId\":3}"));
    }

    @Test
    void createWithoutHeaderIs400() throws Exception {
        expectBadRequest(post("/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Дрель\",\"description\":\"Ударная\",\"available\":true}"));
    }

    @Test
    void updateBlankNameIs400() throws Exception {
        expectBadRequest(json(patch("/items/5"), "{\"name\":\" \"}"));
    }

    @Test
    void updateEmptyDescriptionIs400() throws Exception {
        expectBadRequest(json(patch("/items/5"), "{\"description\":\"\"}"));
    }

    @Test
    void updateOnlyAvailableDelegates() throws Exception {
        when(client.update(anyLong(), anyLong(), any())).thenReturn(ResponseEntity.ok(Map.of("id", 5)));

        mvc.perform(json(patch("/items/5"), "{\"available\":false}"))
                .andExpect(status().isOk());

        verify(client).update(eq(7L), eq(5L), argThat(i -> Boolean.FALSE.equals(i.getAvailable())
                && i.getName() == null));
    }

    @Test
    void getByIdWithoutHeaderCallsClientWithNull() throws Exception {
        when(client.getById(isNull(), eq(5L))).thenReturn(ResponseEntity.ok(Map.of("id", 5)));

        mvc.perform(get("/items/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        verify(client).getById(isNull(), eq(5L));
    }

    @Test
    void getByIdWithHeaderPassesUser() throws Exception {
        when(client.getById(7L, 5L)).thenReturn(ResponseEntity.ok(Map.of("id", 5)));

        mvc.perform(get("/items/5").header(USER_ID_HEADER, 7)).andExpect(status().isOk());

        verify(client).getById(7L, 5L);
    }

    @Test
    void getOwnerItemsDelegates() throws Exception {
        when(client.getOwnerItems(7L)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 5))));

        mvc.perform(get("/items").header(USER_ID_HEADER, 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void getOwnerItemsRequiresHeader() throws Exception {
        expectBadRequest(get("/items"));
    }

    @Test
    void searchDelegatesText() throws Exception {
        when(client.search("дрель")).thenReturn(ResponseEntity.ok(List.of()));

        mvc.perform(get("/items/search").param("text", "дрель")).andExpect(status().isOk());

        verify(client).search("дрель");
    }

    @Test
    void searchWithoutTextPassesEmpty() throws Exception {
        when(client.search("")).thenReturn(ResponseEntity.ok(List.of()));

        mvc.perform(get("/items/search")).andExpect(status().isOk());

        verify(client).search("");
    }

    @Test
    void commentBlankTextIs400() throws Exception {
        expectBadRequest(json(post("/items/5/comment"), "{\"text\":\" \"}"));
    }

    @Test
    void commentValidDelegates() throws Exception {
        when(client.addComment(eq(7L), eq(5L), any())).thenReturn(ResponseEntity.ok(Map.of("id", 1)));

        mvc.perform(json(post("/items/5/comment"), "{\"text\":\"Ок\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(client).addComment(eq(7L), eq(5L), argThat(c -> "Ок".equals(c.getText())));
    }
}
