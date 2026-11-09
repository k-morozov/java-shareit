package ru.practicum.shareit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PatchValidationTest {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private long createUser() throws Exception {
        String email = UUID.randomUUID() + "@mail.ru";
        String body = mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ann\",\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asLong();
    }

    private long createItem(long ownerId) throws Exception {
        String body = mvc.perform(post("/items").header(USER_ID_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Дрель\",\"description\":\"Ударная\",\"available\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(body);
        return node.get("id").asLong();
    }

    @Test
    void patchUserWithBlankNameReturns400() throws Exception {
        long userId = createUser();

        mvc.perform(patch("/users/" + userId).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/users/" + userId)).andExpect(jsonPath("$.name").value("Ann"));
    }

    @Test
    void patchUserWithEmptyEmailReturns400() throws Exception {
        long userId = createUser();

        mvc.perform(patch("/users/" + userId).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchItemWithBlankNameReturns400() throws Exception {
        long userId = createUser();
        long itemId = createItem(userId);

        mvc.perform(patch("/items/" + itemId).header(USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/items/" + itemId)).andExpect(jsonPath("$.name").value("Дрель"));
    }

    @Test
    void patchItemWithEmptyDescriptionReturns400() throws Exception {
        long userId = createUser();
        long itemId = createItem(userId);

        mvc.perform(patch("/items/" + itemId).header(USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchItemWithOnlyAvailableStillWorks() throws Exception {
        long userId = createUser();
        long itemId = createItem(userId);

        mvc.perform(patch("/items/" + itemId).header(USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"available\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.name").value("Дрель"));
    }
}
