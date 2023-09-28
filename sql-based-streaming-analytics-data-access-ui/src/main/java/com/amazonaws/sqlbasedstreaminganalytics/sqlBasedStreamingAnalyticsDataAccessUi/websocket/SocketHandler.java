// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.websocket;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SocketHandler extends TextWebSocketHandler {

    List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketHandler.class);

    @Override
    public void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) {
        LOGGER.info("Received message from client: '{}'", message.getPayload());
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) {
        LOGGER.info("Client connected: {}", session);
        sessions.add(session);
    }

    public void sendMessage(String bla) {
        List<WebSocketSession> closedSessions = new ArrayList<>();
        for (WebSocketSession webSocketSession : sessions) {
            try {
                if (webSocketSession.isOpen()) {
                    webSocketSession.sendMessage(new TextMessage(bla));
                    LOGGER.info("Successfully sent message to client {}", webSocketSession);
                } else {
                    closedSessions.add(webSocketSession);
                }

            } catch (IOException e) {
                LOGGER.warn("Error sending message to client", e);
            }
        }
        sessions.removeAll(closedSessions);
    }
}