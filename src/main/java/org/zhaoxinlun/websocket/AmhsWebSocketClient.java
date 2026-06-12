package org.zhaoxinlun.websocket;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.zhaoxinlun.config.TestbedProperties;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmhsWebSocketClient {

    public static final String MESSAGE_LOGGER_NAME = "AMHS_WEBSOCKET_MESSAGES";

    private static final Logger MESSAGE_LOGGER = LoggerFactory.getLogger(MESSAGE_LOGGER_NAME);

    private final TestbedProperties properties;
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "amhs-websocket-reconnector");
        thread.setDaemon(false);
        return thread;
    });

    private HttpClient httpClient;
    private volatile WebSocket webSocket;
    private volatile boolean running;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!properties.getWebSocket().isEnabled()) {
            log.info("AMHS WebSocket client is disabled.");
            return;
        }

        running = true;
        httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getWebSocket().getConnectTimeout())
                .build();

        log.info("Starting AMHS WebSocket client. url={}, connectTimeout={}, reconnectDelay={}",
                properties.getWebSocket().getUrl(),
                properties.getWebSocket().getConnectTimeout(),
                properties.getWebSocket().getReconnectDelay());
        connect();
    }

    @PreDestroy
    public void stop() {
        running = false;
        reconnectExecutor.shutdownNow();
        WebSocket currentWebSocket = webSocket;
        if (currentWebSocket != null) {
            currentWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "testbed shutting down");
        }
    }

    private void connect() {
        if (!running || !connecting.compareAndSet(false, true)) {
            return;
        }

        URI uri = URI.create(properties.getWebSocket().getUrl());
        httpClient.newWebSocketBuilder()
                .connectTimeout(properties.getWebSocket().getConnectTimeout())
                .buildAsync(uri, new LoggingWebSocketListener())
                .whenComplete((connectedWebSocket, throwable) -> {
                    connecting.set(false);
                    if (throwable != null) {
                        webSocket = null;
                        log.warn("AMHS WebSocket connection failed. url={}, reason={}",
                                uri, mostUsefulMessage(throwable));
                        scheduleReconnect();
                        return;
                    }

                    webSocket = connectedWebSocket;
                    log.info("AMHS WebSocket connected. url={}", uri);
                });
    }

    private void scheduleReconnect() {
        if (!running) {
            return;
        }

        reconnectExecutor.schedule(
                this::connect,
                properties.getWebSocket().getReconnectDelay().toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    private String mostUsefulMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return cause.getClass().getSimpleName() + ": " + message;
    }

    private class LoggingWebSocketListener implements WebSocket.Listener {

        private final StringBuilder messageBuffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuffer.append(data);
            if (last) {
                MESSAGE_LOGGER.info("{}", messageBuffer);
                messageBuffer.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            AmhsWebSocketClient.this.webSocket = null;
            log.info("AMHS WebSocket closed. statusCode={}, reason={}", statusCode, reason);
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            AmhsWebSocketClient.this.webSocket = null;
            log.warn("AMHS WebSocket error. reason={}", mostUsefulMessage(error));
            scheduleReconnect();
        }
    }
}
