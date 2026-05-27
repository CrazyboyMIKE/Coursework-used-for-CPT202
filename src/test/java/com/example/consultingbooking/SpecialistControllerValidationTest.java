package com.example.consultingbooking;

import com.example.consultingbooking.controller.SpecialistController;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.service.AccessAuditService;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.SpecialistService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @MockBean
    private AccessAuditService accessAuditService;

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

    @Test
    void updateOwnProfileDoesNotAcceptMissingCategory() throws Exception {
        Mockito.when(authService.requireUser("token")).thenReturn(new UserAccount());

        mockMvc.perform(put("/api/specialists/me")
                        .header(AuthService.AUTH_HEADER, "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "level": "Certified Advisor",
                                  "baseFee": 120.00,
                                  "bio": "Experienced advisor"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.categoryId").exists());
    }

    @Test
    void forbiddenManagementAccessIsWrittenToAuditService() throws Exception {
        UserAccount actor = new UserAccount();
        Mockito.when(authService.requireUser("token")).thenReturn(actor);
        Mockito.when(specialistService.listSpecialistsForManagement(Mockito.eq(actor), Mockito.anyString(), Mockito.any()))
                .thenThrow(new BusinessException(HttpStatus.FORBIDDEN, "Current user does not have the required role"));

        mockMvc.perform(get("/api/specialists/manage")
                        .header(AuthService.AUTH_HEADER, "token")
                        .param("keyword", "advisor"))
                .andExpect(status().isForbidden());

        Mockito.verify(accessAuditService).recordDeniedAccess(
                "GET",
                "/api/specialists/manage",
                HttpStatus.FORBIDDEN,
                "Current user does not have the required role",
                "token"
        );
    }
}
