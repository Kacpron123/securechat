package org.project.securechat.sharedClass;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.project.securechat.sharedClass.Message;

import java.io.IOException;
import java.time.Instant;

public class MessageTest {

    @Test
    void testMessageCreationAndGetters() {
        String sender = "user1";
        String chat = "general";
        Message.MessageTYPE type = Message.MessageTYPE.TEXT;
        String data = "Hello, world!";
        
        Message message = new Message(sender, chat, type, data);
        
        assertEquals(sender, message.getSenderID());
        assertEquals(chat, message.getChatID());
        assertEquals(type, message.getMessageType());
        assertEquals(data, message.getData());
        assertNotNull(message.getTimestamp());
    }

    @Test
    void testToJSONAndFromJSON() throws IOException {
        String sender = "testUser";
        String chat = "testChat";
        Message.MessageTYPE type = Message.MessageTYPE.COMMAND;
        String data = "/exit";
        
        Message originalMessage = new Message(sender, chat, type, data);
        
        String json = Message.toJSON(originalMessage);
        assertNotNull(json);
        assertTrue(json.contains(sender));
        assertTrue(json.contains(chat));
        assertTrue(json.contains(data));
        assertTrue(json.contains(type.name()));

        try {
            Message deserializedMessage = JsonConverter.parseDataToObject(json,Message.class);
            assertNotNull(deserializedMessage);
            assertEquals(originalMessage.getSenderID(), deserializedMessage.getSenderID());
            assertEquals(originalMessage.getChatID(), deserializedMessage.getChatID());
            assertEquals(originalMessage.getMessageType(), deserializedMessage.getMessageType());
            assertEquals(originalMessage.getData(), deserializedMessage.getData());
        } catch (IOException e) {
            fail("IOException occurred during deserialization: " + e.getMessage());
        }
    }

    @Test
    void testFromJSONInvalidInput() {
        String invalidJson = "{'invalid': 'json'";
        assertThrows(IOException.class, () -> JsonConverter.parseDataToObject(invalidJson, Message.class));
    }
}