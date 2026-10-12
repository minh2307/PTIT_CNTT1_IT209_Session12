package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DemoApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void testHelloEndpoint() {
        DemoApplication app = new DemoApplication();
        assertEquals("Hello IT209 Session 11 - CI Dependency Caching!", app.hello());
    }
}
