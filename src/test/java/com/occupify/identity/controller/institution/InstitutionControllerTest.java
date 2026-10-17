package com.occupify.identity.controller.institution;

import com.occupify.identity.dto.response.profile.InstitutionResponse;
import com.occupify.identity.exception.GlobalExceptionHandler;
import com.occupify.identity.service.institution.InstitutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InstitutionControllerTest {

    @Mock
    private InstitutionService institutionService;

    @InjectMocks
    private InstitutionController institutionController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(institutionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /user/institutions without keyword -> should return 200 OK with top institutions")
    void searchInstitutions_WithoutKeyword_ShouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        InstitutionResponse res = new InstitutionResponse(
                id, "Đại học Bách khoa Hà Nội", "HUST", "Vietnam", "Hà Nội", "https://hust.edu.vn", "https://logo.png"
        );
        when(institutionService.searchInstitutions(null)).thenReturn(List.of(res));

        mockMvc.perform(get("/user/institutions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].code").value("HUST"))
                .andExpect(jsonPath("$.data[0].name").value("Đại học Bách khoa Hà Nội"));

        verify(institutionService).searchInstitutions(null);
    }

    @Test
    @DisplayName("GET /user/institutions?keyword=UIT -> should search by code or name and return 200 OK")
    void searchInstitutions_WithKeyword_ShouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        InstitutionResponse res = new InstitutionResponse(
                id, "Trường Đại học Công nghệ Thông tin – ĐHQG TP.HCM", "UIT", "Vietnam", "TP. Hồ Chí Minh", "https://uit.edu.vn", "https://logo.png"
        );
        when(institutionService.searchInstitutions(eq("UIT"))).thenReturn(List.of(res));

        mockMvc.perform(get("/user/institutions")
                        .param("keyword", "UIT")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].code").value("UIT"))
                .andExpect(jsonPath("$.data[0].name").value("Trường Đại học Công nghệ Thông tin – ĐHQG TP.HCM"));

        verify(institutionService).searchInstitutions("UIT");
    }

    @Test
    @DisplayName("GET /users/institutions?keyword=HUST -> alias should also return 200 OK")
    void searchInstitutions_AliasRoute_ShouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        InstitutionResponse res = new InstitutionResponse(
                id, "Đại học Bách khoa Hà Nội", "HUST", "Vietnam", "Hà Nội", "https://hust.edu.vn", "https://logo.png"
        );
        when(institutionService.searchInstitutions(eq("HUST"))).thenReturn(List.of(res));

        mockMvc.perform(get("/users/institutions")
                        .param("keyword", "HUST")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].code").value("HUST"));

        verify(institutionService).searchInstitutions("HUST");
    }
}
