package com.k44.stn.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
class RequestIdFilterIT {

    @Autowired
    MockMvc mockMvc;

    @Test
    void whenNoXRequestId_thenResponseContainsIt() throws Exception {
        mockMvc.perform(get("/test"))
                .andDo(print())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(header().string("X-Request-Id", not(blankOrNullString())));
    }
}