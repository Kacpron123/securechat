package org.project.securechat.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito; // For mocking Client
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Scanner;
import java.io.IOException;
import java.io.StringReader;
import org.project.securechat.sharedClass.Message;
import org.project.securechat.client.Client; // Assuming Client exists
import org.project.securechat.client.TerminalListener;

public class TerminalListenerTest {

    @Test
    void testProcessingExitCommand() throws IOException, InterruptedException {
        LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();
        Client mockClient = Mockito.mock(Client.class); // Mock the client
        Scanner testScanner = new Scanner(new StringReader("/exit")); // Use StringReader for input

        TerminalListener listener = new TerminalListener(testScanner, queue, "testUser", mockClient);
        // Call the private processing method via reflection or make it package-private for testing
        // For simplicity, let's assume we call it directly here.
        // A better approach for private methods is to test them through the public run() method.
        Message exitMessage = TestUtil.callProcessing(listener, "/exit"); // Assuming a helper for reflection

        assertNotNull(exitMessage);
        assertEquals(Message.MessageTYPE.COMMAND, exitMessage.getMessageType());
        assertEquals("/exit", exitMessage.getData());
        assertEquals("testUser", exitMessage.getSenderID());
        Mockito.verify(mockClient).initiateShutDown(); // Verify client shutdown was called
    }

    // ... more tests for /chat, /file, regular messages
}
// Helper for reflection (place in a separate TestUtil class)
class TestUtil {
    public static Message callProcessing(TerminalListener listener, String message) throws Exception {
        java.lang.reflect.Method method = TerminalListener.class.getDeclaredMethod("processing", String.class);
        method.setAccessible(true);
        return (Message) method.invoke(listener, message);
    }
}