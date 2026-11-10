package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemControllerTest {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private long ownerId;
    private long itemId;

    @BeforeEach
    void setUp() throws Exception {
        String user = mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ann\",\"email\":\"" + UUID.randomUUID() + "@mail.ru\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        ownerId = mapper.readTree(user).get("id").asLong();
        String item = mvc.perform(post("/items").header(USER_ID_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Дрель\",\"description\":\"Ударная\",\"available\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        itemId = mapper.readTree(item).get("id").asLong();
    }

    @Test
    void getItemWithoutHeaderHasNoDates() throws Exception {
        mvc.perform(get("/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId))
                .andExpect(jsonPath("$.lastBooking").isEmpty())
                .andExpect(jsonPath("$.nextBooking").isEmpty())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments").isEmpty());
    }

    @Test
    void commentBlankTextIs400() throws Exception {
        mvc.perform(post("/items/" + itemId + "/comment").header(USER_ID_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }
}
