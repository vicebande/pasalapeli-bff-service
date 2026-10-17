package com.pasalapeli.bff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "azure.activedirectory.enabled=false",
    "services.movie-service.url=http://localhost:8082",
    "services.ticket-service.url=http://localhost:8083"
})
class BffServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
