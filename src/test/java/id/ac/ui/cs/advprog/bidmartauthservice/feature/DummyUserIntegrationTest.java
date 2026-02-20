package id.ac.ui.cs.advprog.bidmartauthservice.feature;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DummyUserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testShowDummyPage() throws Exception {
        mockMvc.perform(get("/dummy"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Test Registrasi User")))
                .andExpect(content().string(containsString("Admin Auth")));
    }

    @Test
    void testRegisterNewUserSuccess() throws Exception {
        mockMvc.perform(post("/dummy/register")
                        .param("nama", "User Testing CI"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dummy"));

        mockMvc.perform(get("/dummy"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User Testing CI")));
    }
}