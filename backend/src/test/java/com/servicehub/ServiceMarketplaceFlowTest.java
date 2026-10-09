package com.servicehub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehub.model.User;
import com.servicehub.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ServiceMarketplaceFlowTest {
    @Autowired private MockMvc api;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository users;

    @Test
    public void customerAndWorkersCompleteTheBiddingFlow() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Account customer = account("CUSTOMER", "customer-" + suffix);
        Account worker500 = account("WORKER", "worker500-" + suffix);
        Account worker400 = account("WORKER", "worker400-" + suffix);
        Account admin = createInitialAdmin("admin-" + suffix);

        api.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Public Admin\",\"email\":\"public-admin-" + suffix + "@example.test\",\"password\":\"TestPass123!\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        api.perform(post("/api/users/admins").header("Authorization", bearer(customer.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unauthorized Admin\",\"email\":\"unauthorized-" + suffix + "@example.test\",\"password\":\"TestPass123!\"}"))
                .andExpect(status().isForbidden());
        api.perform(post("/api/users/admins").header("Authorization", bearer(admin.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Approved Admin\",\"email\":\"approved-" + suffix + "@example.test\",\"password\":\"TestPass123!\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("ADMIN"));

        MvcResult serviceResult = api.perform(post("/api/services").header("Authorization", bearer(admin.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"AC Repair\",\"description\":\"Air conditioner repairs\"}"))
                .andExpect(status().isCreated()).andReturn();
        String serviceId = read(serviceResult).path("_id").asText();

        MvcResult requestResult = api.perform(post("/api/requests").header("Authorization", bearer(customer.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"service\":\"" + serviceId + "\",\"description\":\"My AC is not cooling\",\"location\":\"Test area\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN")).andReturn();
        String requestId = read(requestResult).path("_id").asText();

        api.perform(get("/api/requests").header("Authorization", bearer(worker500.token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*]._id").value(hasItem(requestId)));
        bid(worker500.token, requestId, 500);
        api.perform(get("/api/requests/" + requestId).header("Authorization", bearer(worker400.token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.service.name").value("AC Repair"));
        bid(worker400.token, requestId, 400);

        api.perform(get("/api/bids/request/" + requestId).header("Authorization", bearer(customer.token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].price").value(400.0)).andExpect(jsonPath("$[1].price").value(500.0));
        String cheaperBidId = api.perform(get("/api/bids/request/" + requestId).header("Authorization", bearer(customer.token)))
                .andReturn().getResponse().getContentAsString();
        String selectedBidId = json.readTree(cheaperBidId).get(0).get("_id").asText();

        api.perform(put("/api/bids/" + selectedBidId + "/select").header("Authorization", bearer(customer.token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.request.status").value("SELECTED"));
        api.perform(get("/api/requests/" + requestId).header("Authorization", bearer(customer.token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SELECTED"));
        api.perform(get("/api/requests/my-jobs").header("Authorization", bearer(worker400.token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].price").value(400.0))
                .andExpect(jsonPath("$[0].request._id").value(requestId));
    }

    private Account account(String role, String prefix) throws Exception {
        String email = prefix + "@example.test";
        api.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test " + role + "\",\"email\":\"" + email + "\",\"password\":\"TestPass123!\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isCreated());
        MvcResult login = api.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"TestPass123!\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode result = read(login);
        return new Account(result.path("token").asText());
    }

    private Account createInitialAdmin(String prefix) throws Exception {
        String email = prefix + "@example.test";
        User user = new User();
        user.setName("Test ADMIN");
        user.setEmail(email);
        user.setPassword(new BCryptPasswordEncoder().encode("TestPass123!"));
        user.setRole("ADMIN");
        users.save(user);
        MvcResult login = api.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"TestPass123!\"}"))
                .andExpect(status().isOk()).andReturn();
        return new Account(read(login).path("token").asText());
    }

    private void bid(String token, String requestId, int price) throws Exception {
        api.perform(post("/api/bids").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"request\":\"" + requestId + "\",\"price\":" + price + ",\"message\":\"I can do this job.\"}"))
                .andExpect(status().isCreated());
    }

    private JsonNode read(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()); }
    private String bearer(String token) { return "Bearer " + token; }
    private static class Account { private final String token; private Account(String token) { this.token = token; } }
}
