package ru.practicum.shareit.user;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
    private UserService service;

    private static UserDto ann() {
        return UserDto.builder().id(1L).name("Ann").email("ann@mail.ru").build();
    }

    @Test
    void createReturns201() throws Exception {
        when(service.create(any())).thenReturn(ann());

        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ann\",\"email\":\"ann@mail.ru\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ann@mail.ru"));
    }

    @Test
    void createWithBlankNamePassesToService() throws Exception {
        when(service.create(any())).thenReturn(ann());

        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"email\":\"x\"}"))
                .andExpect(status().isCreated());

        verify(service).create(argThat(u -> " ".equals(u.getName()) && "x".equals(u.getEmail())));
    }

    @Test
    void updateGetGetAllDeleteDelegate() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(ann());
        when(service.getById(1L)).thenReturn(ann());
        when(service.getAll()).thenReturn(List.of(ann()));

        mvc.perform(patch("/users/1").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ann\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ann"));
        mvc.perform(get("/users/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(get("/users")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(delete("/users/1")).andExpect(status().isNoContent());

        verify(service).update(eq(1L), argThat(u -> "Ann".equals(u.getName())));
        verify(service).delete(1L);
    }

    @Test
    void notFoundIs404() throws Exception {
        when(service.getById(9L)).thenThrow(new NotFoundException("User with id=9 not found"));

        mvc.perform(get("/users/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User with id=9 not found"));
    }

    @Test
    void conflictIs409() throws Exception {
        when(service.create(any())).thenThrow(new ConflictException("Email ann@mail.ru already used"));

        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ann\",\"email\":\"ann@mail.ru\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email ann@mail.ru already used"));
    }

    @Test
    void unexpectedErrorIs500() throws Exception {
        when(service.getAll()).thenThrow(new IllegalStateException("boom"));

        mvc.perform(get("/users"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
