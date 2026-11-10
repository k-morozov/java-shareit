package ru.practicum.shareit.booking;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import ru.practicum.shareit.booking.dto.BookingState;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BookingClient client;

    private static String at(LocalDateTime time) {
        return "\"" + time.format(FORMAT) + "\"";
    }

    private static MockHttpServletRequestBuilder create(String start, String end) {
        return post("/bookings").header(USER_ID_HEADER, 7).contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemId\":1,\"start\":" + start + ",\"end\":" + end + "}");
    }

    private void expectBadRequest(MockHttpServletRequestBuilder request) throws Exception {
        mvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(client);
    }

    private void stubCreate() {
        when(client.create(eq(7L), any())).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", 1)));
    }

    @Test
    void createValidDelegates() throws Exception {
        stubCreate();
        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);

        mvc.perform(create(at(start), at(start.plusDays(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(client).create(eq(7L), argThat(b -> b.getItemId() == 1L && start.equals(b.getStart())));
    }

    @Test
    void createNullStartIs400() throws Exception {
        expectBadRequest(create("null", at(LocalDateTime.now().plusDays(2))));
    }

    @Test
    void createNullEndIs400() throws Exception {
        expectBadRequest(create(at(LocalDateTime.now().plusDays(1)), "null"));
    }

    @Test
    void createNullItemIdIs400() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        expectBadRequest(post("/bookings").header(USER_ID_HEADER, 7).contentType(MediaType.APPLICATION_JSON)
                .content("{\"start\":" + at(start) + ",\"end\":" + at(start.plusDays(1)) + "}"));
    }

    @Test
    void createStartInPastIs400() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        expectBadRequest(create(at(now.minusHours(1)), at(now.plusDays(1))));
    }

    @Test
    void createEndInPastIs400() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        expectBadRequest(create(at(now.minusDays(2)), at(now.minusDays(1))));
    }

    @Test
    void createEndBeforeStartIs400() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        expectBadRequest(create(at(start), at(start.minusDays(1))));
    }

    @Test
    void createEndEqualsStartIs400() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        expectBadRequest(create(at(start), at(start)));
    }

    @Test
    void createStartWithinToleranceDelegates() throws Exception {
        stubCreate();
        LocalDateTime now = LocalDateTime.now();

        mvc.perform(create(at(now.minusSeconds(2)), at(now.plusHours(1))))
                .andExpect(status().isCreated());
    }

    @Test
    void createWithoutHeaderIs400() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        expectBadRequest(post("/bookings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemId\":1,\"start\":" + at(start) + ",\"end\":" + at(start.plusDays(1)) + "}"));
    }

    @Test
    void stateIsCaseInsensitive() throws Exception {
        when(client.getByBooker(7L, BookingState.WAITING)).thenReturn(ResponseEntity.ok(List.of()));
        when(client.getByOwner(7L, BookingState.WAITING)).thenReturn(ResponseEntity.ok(List.of()));

        mvc.perform(get("/bookings").header(USER_ID_HEADER, 7).param("state", "waiting"))
                .andExpect(status().isOk());
        mvc.perform(get("/bookings/owner").header(USER_ID_HEADER, 7).param("state", "Waiting"))
                .andExpect(status().isOk());

        verify(client).getByBooker(7L, BookingState.WAITING);
        verify(client).getByOwner(7L, BookingState.WAITING);
    }

    @Test
    void unknownStateIs400() throws Exception {
        mvc.perform(get("/bookings").header(USER_ID_HEADER, 7).param("state", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Unknown state: FOO")));
        mvc.perform(get("/bookings/owner").header(USER_ID_HEADER, 7).param("state", "FOO"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(client);
    }

    @Test
    void defaultStateIsAll() throws Exception {
        when(client.getByBooker(7L, BookingState.ALL)).thenReturn(ResponseEntity.ok(List.of()));
        when(client.getByOwner(7L, BookingState.ALL)).thenReturn(ResponseEntity.ok(List.of()));

        mvc.perform(get("/bookings").header(USER_ID_HEADER, 7)).andExpect(status().isOk());
        mvc.perform(get("/bookings/owner").header(USER_ID_HEADER, 7)).andExpect(status().isOk());

        verify(client).getByBooker(7L, BookingState.ALL);
        verify(client).getByOwner(7L, BookingState.ALL);
    }

    @Test
    void approveRequiresApprovedParam() throws Exception {
        expectBadRequest(patch("/bookings/1").header(USER_ID_HEADER, 7));
    }

    @Test
    void approveNonBooleanIs400() throws Exception {
        expectBadRequest(patch("/bookings/1").header(USER_ID_HEADER, 7).param("approved", "maybe"));
    }

    @Test
    void approveDelegates() throws Exception {
        when(client.approve(7L, 1L, true)).thenReturn(ResponseEntity.ok(Map.of("status", "APPROVED")));

        mvc.perform(patch("/bookings/1").header(USER_ID_HEADER, 7).param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(client).approve(7L, 1L, true);
    }

    @Test
    void getByIdDelegates() throws Exception {
        when(client.getById(7L, 1L)).thenReturn(ResponseEntity.ok(Map.of("id", 1)));

        mvc.perform(get("/bookings/1").header(USER_ID_HEADER, 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
