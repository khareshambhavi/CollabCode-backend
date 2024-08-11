package com.example.websocket.handler;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.example.websocket.Actions;

public class CustomWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, String> userSocketMap = new HashMap<>();
    private static final Map<String, Set<WebSocketSession>> roomSessionsMap = new HashMap<>();
    private static final Map<String, String> roomCodeMap = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Socket connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        
        JsonNode jsonMessage = objectMapper.readTree(message.getPayload());

        if (jsonMessage.has("type")) {
            String type = jsonMessage.get("type").asText();

            switch (type) {
                case Actions.JOIN:
                    handleJoin(session, jsonMessage);
                    break;
                case Actions.CODE_CHANGE:
                    handleCodeChange(session, jsonMessage);
                    break;
                case Actions.LEAVE:
                    handleLeave(session, jsonMessage);
                    break;  
                case Actions.SYNC_CODE:
                    handleSyncCode(session, jsonMessage);
                    break;  
            }
        }
    }


    private void handleJoin(WebSocketSession session, JsonNode jsonMessage) throws Exception {
    
        
            String roomId = jsonMessage.get("roomId").asText();
            String username = jsonMessage.get("username").asText();
    
            // Storing the user's socket ID and username
            userSocketMap.put(session.getId(), username);
            roomSessionsMap.computeIfAbsent(roomId, k -> new HashSet<>()).add(session);
    
            // Preparing the list of clients in the room
            Set<WebSocketSession> clients = roomSessionsMap.get(roomId);
            List<Map<String, String>> clientList = new ArrayList<>();
            for (WebSocketSession client : clients) {
                Map<String, String> clientInfo = new HashMap<>();
                clientInfo.put("socketId", client.getId());
                clientInfo.put("username", userSocketMap.get(client.getId()));
                clientList.add(clientInfo);
            }

            roomCodeMap.putIfAbsent(roomId, ""); 
    
            // Notifying all clients in the room
            for (WebSocketSession client : clients) {
                Map<String, Object> joinResponse = new HashMap<>();
                joinResponse.put("type", Actions.JOINED);
                joinResponse.put("clients", clientList);
                joinResponse.put("username", username);
                joinResponse.put("socketId", session.getId());
                joinResponse.put("code", roomCodeMap.get(roomId)); 

                String response = objectMapper.writeValueAsString(joinResponse);
                client.sendMessage(new TextMessage(response));
          
            }
    }

    private void handleCodeChange(WebSocketSession session, JsonNode jsonMessage) throws Exception {
        String roomId = jsonMessage.get("roomId").asText();
        String code = jsonMessage.get("code").asText();

        roomCodeMap.put(roomId, code); 

        // Broadcasting code change to all clients in the room
        Set<WebSocketSession> clients = roomSessionsMap.get(roomId);
        if (clients != null) {
            for (WebSocketSession client : clients) {
                if (!client.getId().equals(session.getId())) {
                    client.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                            "type", Actions.CODE_CHANGE,
                            "code", code
                    ))));
                }
            }
        }
    }

    private void handleSyncCode(WebSocketSession session, JsonNode jsonMessage) throws Exception {
        String socketId = jsonMessage.get("socketId").asText();
        String code = jsonMessage.get("code").asText();

        // Finding the session with the given socketId and sending the code change message
        for (WebSocketSession client : roomSessionsMap.values().stream().flatMap(Set::stream).collect(Collectors.toSet())) {
            if (client.getId().equals(socketId)) {
                client.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", Actions.CODE_CHANGE,
                        "code", code
                ))));
                break;
            }
        }
    }

    private void handleLeave(WebSocketSession session, JsonNode jsonMessage) throws Exception {
        String roomId = jsonMessage.get("roomId").asText();
        String username = jsonMessage.get("username").asText();
    
        // Removing the user from the room
        Set<WebSocketSession> clients = roomSessionsMap.get(roomId);
        if (clients != null) {
            clients.remove(session);
        }
    
        userSocketMap.remove(session.getId());
    
        // Notifying all clients in the room about the disconnection
        if (clients != null) {
            for (WebSocketSession client : clients) {
                client.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", Actions.DISCONNECTED,
                        "socketId", session.getId(),
                        "username", username,
                        "clients", clients.stream()
                            .map(c -> Map.of("socketId", c.getId(), "username", userSocketMap.get(c.getId())))
                            .collect(Collectors.toList())
                ))));
            }
        }
    }
    

  
    

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = userSocketMap.get(session.getId());

        // Removing the user from the userSocketMap and roomSessionsMap
        userSocketMap.remove(session.getId());
        for (Set<WebSocketSession> sessions : roomSessionsMap.values()) {
            sessions.remove(session);
        }

        // Notifying all rooms the user was part of about the disconnection
        for (Map.Entry<String, Set<WebSocketSession>> entry : roomSessionsMap.entrySet()) {
            String roomId = entry.getKey();
            Set<WebSocketSession> clients = entry.getValue();

            List<Map<String, String>> clientList = new ArrayList<>();
            for (WebSocketSession client : clients) {
                Map<String, String> clientInfo = new HashMap<>();
                clientInfo.put("socketId", client.getId());
                clientInfo.put("username", userSocketMap.get(client.getId()));
                clientList.add(clientInfo);
            }

            for (WebSocketSession client : clients) {
                client.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", Actions.DISCONNECTED,
                        "socketId", session.getId(),
                        "username", username,
                        "clients", clientList
                ))));
            }
        }

        System.out.println("Socket disconnected: " + session.getId());
    }
}
