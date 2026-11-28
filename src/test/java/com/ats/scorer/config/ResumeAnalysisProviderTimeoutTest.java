package com.ats.scorer.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeAnalysisProviderTimeoutTest {

    @Test
    void appliesConfiguredReadTimeoutToProviderHttpRequests() throws IOException, InterruptedException {
        CountDownLatch requestStarted = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/slow", exchange -> {
            requestStarted.countDown();
            try {
                Thread.sleep(1_000);
                byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream responseBody = exchange.getResponseBody()) {
                    responseBody.write(response);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
                // The client is expected to close the connection after its read timeout.
            }
        });
        server.start();

        try {
            RestClient.Builder restClientBuilder = new ResumeAnalysisProviderConfiguration()
                    .resumeAnalysisRestClientBuilder("1s", "150ms");

            assertThrows(ResourceAccessException.class, () -> restClientBuilder.build()
                    .get()
                    .uri("http://127.0.0.1:%d/slow".formatted(server.getAddress().getPort()))
                    .retrieve()
                    .body(String.class));
            assertTrue(requestStarted.await(1, TimeUnit.SECONDS));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsNonPositiveProviderTimeouts() {
        ResumeAnalysisProviderConfiguration configuration = new ResumeAnalysisProviderConfiguration();

        assertThrows(IllegalArgumentException.class, () -> configuration
                .resumeAnalysisRestClientBuilder("0s", "1s"));
        assertThrows(IllegalArgumentException.class, () -> configuration
                .resumeAnalysisRestClientBuilder("1s", "0s"));
    }
}