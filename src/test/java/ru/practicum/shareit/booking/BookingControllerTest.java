package ru.practicum.shareit.booking;

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
class BookingControllerTest {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private long ownerId;
    private long bookerId;
    private long itemId;

    @BeforeEach
    void setUp() throws Exception {
        ownerId = createUser();
        bookerId = createUser();
        String body = mvc.perform(post("/items").header(USER_ID_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Дрель\",\"description\":\"Ударная\",\"available\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        itemId = mapper.readTree(body).get("id").asLong();
    }

    private long createUser() throws Exception {
        String body = mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ann\",\"email\":\"" + UUID.randomUUID() + "@mail.ru\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asLong();
    }

    private String booking(String start, String end) {
        return "{\"itemId\":" + itemId + ",\"start\":" + start + ",\"end\":" + end + "}";
    }

    @Test
    void createResponseEchoesStartEndFormat() throws Exception {
        mvc.perform(post("/bookings").header(USER_ID_HEADER, bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(booking("\"2030-01-01T10:00:00\"", "\"2030-01-02T10:00:00\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.start").value("2030-01-01T10:00:00"))
                .andExpect(jsonPath("$.end").value("2030-01-02T10:00:00"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.item.id").value(itemId))
                .andExpect(jsonPath("$.item.name").value("Дрель"))
                .andExpect(jsonPath("$.booker.id").value(bookerId));
    }

    @Test
    void createWithNullStartIs400() throws Exception {
        mvc.perform(post("/bookings").header(USER_ID_HEADER, bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(booking("null", "\"2030-01-02T10:00:00\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithoutHeaderIs400() throws Exception {
        mvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(booking("\"2030-01-01T10:00:00\"", "\"2030-01-02T10:00:00\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stateIsCaseInsensitiveAndUnknownIs400() throws Exception {
        mvc.perform(post("/bookings").header(USER_ID_HEADER, bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(booking("\"2030-01-01T10:00:00\"", "\"2030-01-02T10:00:00\"")))
                .andExpect(status().isCreated());

        mvc.perform(get("/bookings").header(USER_ID_HEADER, bookerId).param("state", "waiting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/bookings/owner").header(USER_ID_HEADER, ownerId).param("state", "Waiting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/bookings").header(USER_ID_HEADER, bookerId).param("state", "FOO"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/bookings/owner").header(USER_ID_HEADER, ownerId).param("state", "FOO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listWithoutStateDefaultsToAll() throws Exception {
        mvc.perform(post("/bookings").header(USER_ID_HEADER, bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(booking("\"2030-01-01T10:00:00\"", "\"2030-01-02T10:00:00\"")))
                .andExpect(status().isCreated());

        mvc.perform(get("/bookings").header(USER_ID_HEADER, bookerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].item.id").value(itemId));
        mvc.perform(get("/bookings/owner").header(USER_ID_HEADER, ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].booker.id").value(bookerId));
    }
}
