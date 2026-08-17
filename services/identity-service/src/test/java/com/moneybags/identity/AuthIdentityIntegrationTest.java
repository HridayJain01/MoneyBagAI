package com.moneybags.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql(statements = "MERGE INTO roles (role_name, description) KEY(role_name) "
        + "VALUES ('CUSTOMER', 'Self-registered customer')")
class AuthIdentityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void registrationEmailLoginCookieAndCurrentProfileWorkTogether() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Asha",
                                  "lastName": "Shah",
                                  "email": "Asha.Shah@example.com",
                                  "password": "Password@123",
                                  "dob": "1995-06-15",
                                  "gender": "FEMALE",
                                  "mobile": "9876543210"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registered successfully"))
                .andExpect(jsonPath("$.user.email").value("asha.shah@example.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("CUSTOMER"));

        String loginResponse = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"asha.shah@example.com","password":"Password@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("access-token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andReturn().getResponse().getContentAsString();

        long userId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(loginResponse).get("userId").asLong();
        mvc.perform(get("/api/v1/users/me").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Asha"))
                .andExpect(jsonPath("$.permissions").isArray());
    }

    @Test
    void roleAdministrationRequiresTheIdentityManagementPermission() throws Exception {
        String body = """
                {"name":"AUDITOR","description":"Read-only audit user"}
                """;

        mvc.perform(post("/api/v1/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_REQUIRED"));

        mvc.perform(post("/api/v1/admin/roles")
                        .header("X-Permissions", "USER_MANAGE,ROLE_PERMISSION_MANAGE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roleName").value("AUDITOR"));
    }
}
