package ru.practicum.shareit.user;

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
import org.springframework.web.client.ResourceAccessException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserClient client;

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private void expectBadRequest(MockHttpServletRequestBuilder request) throws Exception {
        mvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(client);
    }

    @Test
    void createValidDelegatesAndReturns201() throws Exception {
        when(client.create(any())).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", 1)));

        mvc.perform(json(post("/users"), "{\"name\":\"Ann\",\"email\":\"ann@mail.ru\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(client).create(argThat(u -> "Ann".equals(u.getName()) && "ann@mail.ru".equals(u.getEmail())));
    }

    @Test
    void createWithoutEmailIs400() throws Exception {
        expectBadRequest(json(post("/users"), "{\"name\":\"Ann\"}"));
    }

    @Test
    void createWithInvalidEmailIs400() throws Exception {
        expectBadRequest(json(post("/users"), "{\"name\":\"Ann\",\"email\":\"ann.mail.ru\"}"));
    }

    @Test
    void createWithBlankNameIs400() throws Exception {
        expectBadRequest(json(post("/users"), "{\"name\":\" \",\"email\":\"ann@mail.ru\"}"));
    }

    @Test
    void updateBlankNameIs400() throws Exception {
        expectBadRequest(json(patch("/users/1"), "{\"name\":\"  \"}"));
    }

    @Test
    void updateEmptyEmailIs400() throws Exception {
        expectBadRequest(json(patch("/users/1"), "{\"email\":\"\"}"));
    }

    @Test
    void updateEmailWithoutAtIs400() throws Exception {
        expectBadRequest(json(patch("/users/1"), "{\"email\":\"ann.mail.ru\"}"));
    }

    @Test
    void updatePartialDelegates() throws Exception {
        when(client.update(anyLong(), any())).thenReturn(ResponseEntity.ok(Map.of("id", 1, "name", "Bob")));

        mvc.perform(json(patch("/users/1"), "{\"name\":\"Bob\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bob"));

        verify(client).update(eq(1L), argThat(u -> "Bob".equals(u.getName()) && u.getEmail() == null));
    }

    @Test
    void getByIdGetAllDeleteDelegate() throws Exception {
        when(client.getById(1L)).thenReturn(ResponseEntity.ok(Map.of("id", 1)));
        when(client.getAll()).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 1))));
        when(client.delete(1L)).thenReturn(ResponseEntity.noContent().build());

        mvc.perform(get("/users/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(get("/users")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(delete("/users/1")).andExpect(status().isNoContent());

        verify(client).getById(1L);
        verify(client).getAll();
        verify(client).delete(1L);
    }

    @Test
    void serverErrorStatusIsPassedThrough() throws Exception {
        when(client.create(any())).thenReturn(ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Email ann@mail.ru already used\"}"));

        mvc.perform(json(post("/users"), "{\"name\":\"Ann\",\"email\":\"ann@mail.ru\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email ann@mail.ru already used"));
    }

    @Test
    void serverUnavailableIs500() throws Exception {
        when(client.getAll()).thenThrow(new ResourceAccessException("Connection refused"));

        mvc.perform(get("/users"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void nonNumericUserIdIs400() throws Exception {
        expectBadRequest(get("/users/abc"));
    }
}
