package ru.practicum.shareit.request;

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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ItemRequestClient client;

    private static MockHttpServletRequestBuilder create(String body) {
        return post("/requests").header(USER_ID_HEADER, 7).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private void expectBadRequest(MockHttpServletRequestBuilder request) throws Exception {
        mvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(client);
    }

    @Test
    void createValidDelegates() throws Exception {
        when(client.create(eq(7L), any())).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", 5)));

        mvc.perform(create("{\"description\":\"Нужна дрель\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));

        verify(client).create(eq(7L), argThat(d -> "Нужна дрель".equals(d.getDescription())));
    }

    @Test
    void createBlankDescriptionIs400() throws Exception {
        expectBadRequest(create("{\"description\":\" \"}"));
    }

    @Test
    void createWithoutDescriptionIs400() throws Exception {
        expectBadRequest(create("{}"));
    }

    @Test
    void createWithoutHeaderIs400() throws Exception {
        expectBadRequest(post("/requests").contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Нужна дрель\"}"));
    }

    @Test
    void getOwnAllByIdDelegate() throws Exception {
        when(client.getOwn(7L)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 1))));
        when(client.getAll(7L)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 2))));
        when(client.getById(7L, 5L)).thenReturn(ResponseEntity.ok(Map.of("id", 5)));

        mvc.perform(get("/requests").header(USER_ID_HEADER, 7)).andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(get("/requests/all").header(USER_ID_HEADER, 7)).andExpect(jsonPath("$[0].id").value(2));
        mvc.perform(get("/requests/5").header(USER_ID_HEADER, 7)).andExpect(jsonPath("$.id").value(5));

        verify(client).getOwn(7L);
        verify(client).getAll(7L);
        verify(client).getById(7L, 5L);
    }

    @Test
    void nonNumericRequestIdIs400() throws Exception {
        expectBadRequest(get("/requests/abc").header(USER_ID_HEADER, 7));
    }
}
