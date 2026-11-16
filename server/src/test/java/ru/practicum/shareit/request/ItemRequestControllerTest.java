package ru.practicum.shareit.request;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.request.dto.ItemAnswerDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
    private ItemRequestService service;

    private static ItemRequestDto dto() {
        return ItemRequestDto.builder()
                .id(5L)
                .description("Нужна дрель")
                .created(LocalDateTime.of(2030, 1, 1, 10, 0))
                .items(List.of(new ItemAnswerDto(3L, "Дрель", 2L)))
                .build();
    }

    @Test
    void createReturns201AndBody() throws Exception {
        when(service.create(eq(7L), any())).thenReturn(dto());

        mvc.perform(post("/requests").header(USER_ID_HEADER, 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Нужна дрель\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.description").value("Нужна дрель"));

        verify(service).create(eq(7L), argThat(d -> "Нужна дрель".equals(d.getDescription())));
    }

    @Test
    void getOwnDelegatesWithHeader() throws Exception {
        when(service.getOwn(7L)).thenReturn(List.of(dto()));

        mvc.perform(get("/requests").header(USER_ID_HEADER, 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].items[0].ownerId").value(2))
                .andExpect(jsonPath("$[0].items[0].name").value("Дрель"));

        verify(service).getOwn(7L);
    }

    @Test
    void getAllDelegates() throws Exception {
        when(service.getOthers(7L)).thenReturn(List.of(dto()));

        mvc.perform(get("/requests/all").header(USER_ID_HEADER, 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));

        verify(service).getOthers(7L);
    }

    @Test
    void getByIdDelegates() throws Exception {
        when(service.getById(7L, 5L)).thenReturn(dto());

        mvc.perform(get("/requests/5").header(USER_ID_HEADER, 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(3));

        verify(service).getById(7L, 5L);
    }

    @Test
    void notFoundFromServiceIs404() throws Exception {
        when(service.getById(7L, 5L)).thenThrow(new NotFoundException("Request with id=5 not found"));

        mvc.perform(get("/requests/5").header(USER_ID_HEADER, 7))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Request with id=5 not found"));
    }
}
