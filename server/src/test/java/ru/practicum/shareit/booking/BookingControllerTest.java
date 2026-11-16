package ru.practicum.shareit.booking;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.ConflictException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BookingService service;

    private static BookingDto booking(BookingStatus status) {
        return BookingDto.builder().id(1L)
                .start(LocalDateTime.of(2030, 1, 1, 10, 0)).end(LocalDateTime.of(2030, 1, 2, 10, 0))
                .status(status).item(new BookingDto.ItemRef(5L, "Дрель")).booker(new BookingDto.BookerRef(7L))
                .build();
    }

    @Test
    void createPassesNullDatesToService() throws Exception {
        when(service.create(eq(7L), any())).thenReturn(booking(BookingStatus.WAITING));

        mvc.perform(post("/bookings").header(USER_ID_HEADER, 7).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING"));

        verify(service).create(eq(7L), argThat(b -> b.getItemId() == 1L && b.getStart() == null));
    }

    @Test
    void approveGetByIdListsDelegate() throws Exception {
        when(service.approve(7L, 1L, true)).thenReturn(booking(BookingStatus.APPROVED));
        when(service.getById(7L, 1L)).thenReturn(booking(BookingStatus.APPROVED));
        when(service.getByBooker(7L, BookingState.WAITING)).thenReturn(List.of(booking(BookingStatus.WAITING)));
        when(service.getByOwner(7L, BookingState.ALL)).thenReturn(List.of(booking(BookingStatus.APPROVED)));

        mvc.perform(patch("/bookings/1").header(USER_ID_HEADER, 7).param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(get("/bookings/1").header(USER_ID_HEADER, 7))
                .andExpect(jsonPath("$.item.name").value("Дрель"));
        mvc.perform(get("/bookings").header(USER_ID_HEADER, 7).param("state", "WAITING"))
                .andExpect(jsonPath("$[0].booker.id").value(7));
        mvc.perform(get("/bookings/owner").header(USER_ID_HEADER, 7))
                .andExpect(jsonPath("$[0].id").value(1));

        verify(service).getByBooker(7L, BookingState.WAITING);
        verify(service).getByOwner(7L, BookingState.ALL);
    }

    @Test
    void conflictIs409() throws Exception {
        when(service.create(eq(7L), any())).thenThrow(new ConflictException("Already booked"));

        mvc.perform(post("/bookings").header(USER_ID_HEADER, 7).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":1,\"start\":\"2030-01-01T10:00:00\",\"end\":\"2030-01-02T10:00:00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Already booked"));
    }

    @Test
    void badStateIs400() throws Exception {
        mvc.perform(get("/bookings").header(USER_ID_HEADER, 7).param("state", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown state: FOO"));
    }

    @Test
    void nonBooleanApprovedIs400() throws Exception {
        mvc.perform(patch("/bookings/1").header(USER_ID_HEADER, 7).param("approved", "maybe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
