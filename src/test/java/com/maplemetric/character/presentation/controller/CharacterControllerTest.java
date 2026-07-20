package com.maplemetric.character.presentation.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maplemetric.character.application.service.CharacterQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CharacterController.class)
class CharacterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CharacterQueryService characterQueryService;

    @Test
    void 캐릭터명이공백이면400응답을반환한다() throws Exception {
        mockMvc.perform(
                        get("/api/v1/characters/search")
                                .param("characterName", " ")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GLOBAL_001"))
                .andExpect(
                        jsonPath("$.message")
                                .value("요청값이 올바르지 않습니다.")
                )
                .andExpect(jsonPath("$.data").value(nullValue()));

        verifyNoInteractions(characterQueryService);
    }

    @Test
    void 캐릭터명파라미터가없으면400응답을반환한다() throws Exception {
        mockMvc.perform(
                        get("/api/v1/characters/search")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GLOBAL_001"))
                .andExpect(
                        jsonPath("$.message")
                                .value("요청값이 올바르지 않습니다.")
                )
                .andExpect(jsonPath("$.data").value(nullValue()));

        verifyNoInteractions(characterQueryService);
    }
}