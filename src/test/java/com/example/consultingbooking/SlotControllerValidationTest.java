package com.example.consultingbooking;

import com.example.consultingbooking.controller.SlotController;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.service.AccessAuditService;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.SlotService;
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

@WebMvcTest(SlotController.class)
class SlotControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SlotService slotService;

    @MockBean
    private AuthService authService;

    @MockBean
    private AccessAuditService accessAuditService;

    @Test
    void slotListInvalidStatusReturnsTypedError() throws Exception {
        mockMvc.perform(get("/api/slots/specialists/1")
                        .param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"))
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    void slotCreateMissingStartTimeReturnsValidationError() throws Exception {
        Mockito.when(authService.requireUser("token")).thenReturn(new UserAccount());

        mockMvc.perform(post("/api/slots/specialists/1")
                        .header(AuthService.AUTH_HEADER, "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endTime": "2026-05-11T11:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.startTime").exists());
    }
}
