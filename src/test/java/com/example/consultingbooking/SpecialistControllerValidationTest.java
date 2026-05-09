package com.example.consultingbooking;

import com.example.consultingbooking.controller.SpecialistController;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.SpecialistService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpecialistController.class)
class SpecialistControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpecialistService specialistService;

    @MockBean
    private AuthService authService;

    @Test
    void invalidMinFeeQueryParamReturnsTypedError() throws Exception {
        mockMvc.perform(get("/api/specialists")
                        .param("minFee", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"))
                .andExpect(jsonPath("$.fieldErrors.minFee").exists());
    }

    @Test
    void createSpecialistWithNegativeCategoryIdReturnsValidationError() throws Exception {
        Mockito.when(authService.requireUser("token")).thenReturn(new UserAccount());

        mockMvc.perform(post("/api/specialists")
                        .header(AuthService.AUTH_HEADER, "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "categoryId": -1,
                                  "level": "Certified Advisor",
                                  "baseFee": 120.00,
                                  "feeCurrency": "USD",
                                  "status": "ACTIVE",
                                  "bio": "Experienced advisor"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.categoryId").exists());
    }
}
