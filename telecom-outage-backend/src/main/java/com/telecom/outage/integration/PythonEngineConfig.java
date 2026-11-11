package com.telecom.outage.integration;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class PythonEngineConfig {

    @Value("${python.engine.base-url}")
    private String pythonEngineBaseUrl;

    @Value("${python.engine.api-key}")
    private String pythonEngineApiKey;

    @Bean
    public RestClient pythonEngineRestClient(ObjectMapper objectMapper) {

        // ------------------------------------------------------------------
        // CRITICAL: pin the JDK HttpClient to HTTP/1.1.
        //
        // Spring's RestClient.builder() falls back to JdkClientHttpRequestFactory
        // (java.net.http.HttpClient) when Apache HttpClient / Jetty are absent.
        // That client defaults to HTTP/2 and, over plaintext http://, adds:
        //
        //     Connection: Upgrade, HTTP2-Settings
        //     HTTP2-Settings: AAEAAEAAAAIAAAA...
        //     Upgrade: h2c
        //
        // Uvicorn has no HTTP/2 support. It logs "Unsupported upgrade request."
        // and DISCARDS THE REQUEST BODY, so FastAPI replies:
        //     422 {"loc":["body"],"msg":"Field required","input":null}
        // and on the h11 parser path it replies:
        //     400 "Invalid HTTP request received."
        //
        // Forcing HTTP_1_1 removes those three headers entirely.
        // ------------------------------------------------------------------
        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        return RestClient.builder()
                .baseUrl(pythonEngineBaseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(jdkHttpClient))
                // ------------------------------------------------------------------
                // Use the Spring-Boot-configured ObjectMapper for the outgoing body.
                // RestClient.builder() otherwise builds its OWN ObjectMapper, which
                // has no JavaTimeModule -- LocalDateTime would then be serialized as
                // [2026,11,10,10,0] and /alarms/correlate would fail with
                //     422 "Input should be a valid datetime".
                // ------------------------------------------------------------------
                .messageConverters(converters -> {
                    converters.clear();
                    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
                })
                // .trim() guards against a trailing space or newline in the property,
                // which would make the header value illegal and trigger a parser 400.
                .defaultHeader("X-Internal-API-Key", pythonEngineApiKey.trim())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}