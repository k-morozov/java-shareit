package ru.practicum.shareit.item;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithDetailsDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
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
    private ItemService service;

    private static ItemDto drill() {
        return ItemDto.builder().id(5L).name("Дрель").description("Ударная").available(true).requestId(3L).build();
    }

    private static ItemWithDetailsDto details() {
        return ItemWithDetailsDto.builder().id(5L).name("Дрель").description("Ударная").available(true)
                .comments(List.of()).build();
    }

    @Test
    void createPassesBodyWithoutValidation() throws Exception {
        when(service.create(eq(7L), any())).thenReturn(drill());

        mvc.perform(post("/items").header(USER_ID_HEADER, 7).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value(3));

        verify(service).create(eq(7L), argThat(i -> i.getName() == null));
    }

    @Test
    void updateGetByIdOwnerItemsSearchCommentDelegate() throws Exception {
        when(service.update(eq(7L), eq(5L), any())).thenReturn(drill());
        when(service.getById(isNull(), eq(5L))).thenReturn(details());
        when(service.getOwnerItems(7L)).thenReturn(List.of(details()));
        when(service.search("дрель")).thenReturn(List.of(drill()));
        when(service.addComment(eq(7L), eq(5L), any())).thenReturn(CommentDto.builder().id(1L).text("Ок")
                .authorName("Ann").created(LocalDateTime.of(2030, 1, 1, 10, 0)).build());

        mvc.perform(patch("/items/5").header(USER_ID_HEADER, 7).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/items/5")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Дрель"));
        mvc.perform(get("/items").header(USER_ID_HEADER, 7)).andExpect(jsonPath("$[0].id").value(5));
        mvc.perform(get("/items/search").param("text", "дрель")).andExpect(jsonPath("$[0].id").value(5));
        mvc.perform(post("/items/5/comment").header(USER_ID_HEADER, 7).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Ок\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorName").value("Ann"));

        verify(service).update(eq(7L), eq(5L), argThat(i -> Boolean.FALSE.equals(i.getAvailable())));
        verify(service).getById(isNull(), eq(5L));
        verify(service).addComment(eq(7L), eq(5L), argThat(c -> "Ок".equals(c.getText())));
    }

    @Test
    void getByIdWithHeaderPassesUser() throws Exception {
        when(service.getById(7L, 5L)).thenReturn(details());

        mvc.perform(get("/items/5").header(USER_ID_HEADER, 7)).andExpect(status().isOk());

        verify(service).getById(7L, 5L);
    }

    @Test
    void forbiddenIs403() throws Exception {
        when(service.update(eq(7L), eq(5L), any())).thenThrow(new ForbiddenException("Not owner"));

        mvc.perform(patch("/items/5").header(USER_ID_HEADER, 7).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Чужая\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Not owner"));
    }

    @Test
    void validationExceptionIs400() throws Exception {
        when(service.addComment(eq(7L), eq(5L), any())).thenThrow(new ValidationException("No finished booking"));

        mvc.perform(post("/items/5/comment").header(USER_ID_HEADER, 7).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Ок\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No finished booking"));
    }

    @Test
    void missingHeaderIs400() throws Exception {
        mvc.perform(get("/items"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
