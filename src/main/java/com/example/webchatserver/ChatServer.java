package com.example.webchatserver;


import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * This class represents a web socket server, a new connection is created and it receives a roomID as a parameter
 * **/
@ServerEndpoint(value="/ws/{roomID}")
public class ChatServer {

    // contains a static List of ChatRoom used to control the existing rooms and their users
    private static Map<String, ChatRoom> chatRooms = new HashMap<>();
    // you may add other attributes as you see fit
    private Map<String, String> usernames = new HashMap<String, String>();
    private static Map<String, String> roomList = new HashMap<String, String>();
    private static Map<String, String> roomHistoryList = new HashMap<String, String>();

    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) throws IOException, EncodeException {

        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server ): Welcome to the chat room. Please state your username to begin.\"}");
        roomList.put(session.getId(), roomID); // adding userID to a room
    }
      /*  session.getBasicRemote().sendText("First sample message to the client");
//        accessing the roomID parameter
        System.out.println(roomID);*/




    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        String userId = session.getId();
        if (usernames.containsKey(userId)) {
            String username = usernames.get(userId);
            String roomID = roomList.get(userId);
            usernames.remove(userId);
            for (Session peer : session.getOpenSessions()){ //broadcast this person left the server
                if(roomList.get(peer.getId()).equals(roomID)) { // broadcast only to those in the same room
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + username + " left the chat room.\"}");
                }
            }
        }
    }

    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException, EncodeException {
        String userID = session.getId(); // my id
        String roomID = roomList.get(userID); // my room
        JSONObject jsonmsg = new JSONObject(comm);
        String type = (String) jsonmsg.get("type");
        String message = (String) jsonmsg.get("msg");

//        Example conversion of json messages from the client
        //        JSONObject jsonmsg = new JSONObject(comm);
//        String val1 = (String) jsonmsg.get("attribute1");
//        String val2 = (String) jsonmsg.get("attribute2");

        // handle the messages

        if(usernames.containsKey(userID)){ // not their first message
            String username = usernames.get(userID);
            System.out.println(username);
            for(Session peer: session.getOpenSessions()){
                // only send my messages to those in the same room
                if(roomList.get(peer.getId()).equals(roomID)) {
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(" + username + "): " + message + "\"}");
                }
            }
        }else{ //first message is their username
            usernames.put(userID, message);
            session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server ): Welcome, " + message + "!\"}");
            for(Session peer: session.getOpenSessions()){
                // only announce to those in the same room as me, excluding myself
                if((!peer.getId().equals(userID)) && (roomList.get(peer.getId()).equals(roomID))){
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + message + " joined the chat room.\"}");
                }
            }
        }

    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.out.println("Error: " + throwable.getMessage());
    }

    /**
     * Broadcasts a message to all the users of a chat room.
     * @param chatRoom the chat room to which the message will be broadcasted.
     * @param message the message to be broadcasted.
     */
    private void broadcastToRoom(ChatRoom chatRoom, String message) {
        for (String userId : chatRoom.getUsers()) {
            Session peer = chatRoom.getSession(userId);
            if (peer != null && peer.isOpen()) {
                try {
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"" + message + "\"}");
                } catch (IOException e) {
                    System.out.println("Error broadcasting message: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Finds a chat room in the chatRooms list, if it does not exist, it creates a new one.
     * @param roomID the ID of the chat room to be found or created.
     * @return the chat room found or created.
     */
    private ChatRoom findOrCreateChatRoom(String roomID) {
        ChatRoom chatRoom = null;
        for (ChatRoom cr : chatRooms) {
            if (cr.getCode().equals(roomID)) {
                chatRoom = cr;
                break;
            }
        }
        if (chatRoom == null) {
            chatRoom = new ChatRoom(roomID);
            chatRooms.add(chatRoom);
        }
        return chatRoom;
    }



}