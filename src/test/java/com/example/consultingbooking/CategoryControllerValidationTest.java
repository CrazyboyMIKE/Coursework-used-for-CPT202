package com.example.consultingbooking;

import com.example.consultingbooking.controller.CategoryController;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.service.AccessAuditService;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private AuthService authService;

    @MockBean
    private AccessAuditService accessAuditService;

    @Test
    void categoryCreateBlankNameReturnsValidationError() throws Exception {
        Mockito.when(authService.requireUser("token")).thenReturn(new UserAccount());

        mockMvc.perform(post("/api/categories")
                        .header(AuthService.AUTH_HEADER, "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   ",
                                  "description": "Blank category name",
                                  "active": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void categoryUpdateDescriptionTooLongReturnsValidationError() throws Exception {
        Mockito.when(authService.requireUser("token")).thenReturn(new UserAccount());

        mockMvc.perform(put("/api/categories/1")
                        .header(AuthService.AUTH_HEADER, "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Consulting",
                                  "description": "%s",
                                  "active": true
                                }
                                """.formatted("x".repeat(256))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.description").exists());
    }
}
