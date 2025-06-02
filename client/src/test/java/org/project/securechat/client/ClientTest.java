package org.project.securechat.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.securechat.sharedClass.Message; // Assuming Message class is needed for future tests

import java.io.*;
import java.net.InetAddress; // For mocking InetAddress
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientTest {

    // Mocks for network components
    @Mock private Socket mockSocket;
    @Mock private DataInputStream mockSocketIn;
    @Mock private DataOutputStream mockSocketOut;
    // Mock for InetAddress to avoid NullPointerException when Client logs connection info
    @Mock private InetAddress mockInetAddress;

    // To capture System.out
    private ByteArrayOutputStream outputStreamCaptor;
    // To feed System.in
    private InputStream originalIn;
    private PrintStream originalOut;

    private Client client; // The Client instance under test
    private Thread clientMainThread; // Thread to run client.start() in

    @BeforeEach
    void setUp() throws IOException {
        // 1. Redirect System.out and System.in
        originalIn = System.in;
        originalOut = System.out;
        outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));

        // 2. Stub the behavior of mockSocket
        when(mockSocket.getInputStream()).thenReturn(mockSocketIn);
        when(mockSocket.getOutputStream()).thenReturn(mockSocketOut);
        
        // 3. Mock InetAddress for the connection message in Client constructor
        when(mockSocket.getInetAddress()).thenReturn(mockInetAddress);
        when(mockInetAddress.getHostName()).thenReturn("mockhost");
        when(mockSocket.getPort()).thenReturn(12345);
    }

    @AfterEach
    void tearDown() throws IOException, InterruptedException {
        // Ensure client cleanup happens and its main thread terminates
        if (client != null) {
            client.initiateShutDown(); // Trigger client's shutdown logic
        }
        if (clientMainThread != null && clientMainThread.isAlive()) {
            // Give it a moment to finish, then interrupt if needed
            clientMainThread.join(500); // Wait for thread to naturally die
            if (clientMainThread.isAlive()) {
                clientMainThread.interrupt(); // Force interrupt if still alive
                clientMainThread.join(500); // Wait a bit more after interrupt
            }
        }
        
        // Restore System.in and System.out
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Should establish connection and perform successful login")
    void shouldConnectAndLoginSuccessfully() throws Exception {
        // --- Given ---
        // 1. Simulate server responses for the login process
        when(mockSocketIn.readUTF())
                .thenReturn("Enter your login:")      // First prompt for login
                .thenReturn("Enter your password:")     // Second prompt for password
                .thenReturn("Welcome, testuser!");      // Server's welcome message after successful login

        // 2. Simulate user input for login
        String simulatedUserInput = "testuser\npassword123\n"; // Login and password followed by newlines
        System.setIn(new ByteArrayInputStream(simulatedUserInput.getBytes()));

        // --- When ---
        // Instantiate Client, passing the mocked socket
        // The constructor itself will run the login process because login() is called within it.
        client = new Client(mockSocket);

        // Start the client's main loop in a separate thread.
        // We need a latch to know when the client's start() method begins,
        // so we can give it time to process and then shut it down cleanly.
        CountDownLatch clientStartLatch = new CountDownLatch(1);
        clientMainThread = new Thread(() -> {
            try {
                clientStartLatch.countDown(); // Signal that client.start() has been called
                client.start(); // This will block on clientOutputQueue.take()
            } catch (Exception e) {
                // Log any unexpected exceptions from the client's main loop
                System.err.println("Client main thread caught unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        });
        clientMainThread.start();

        // Wait until the client's start() method is confirmed to have begun execution.
        assertTrue(clientStartLatch.await(1, TimeUnit.SECONDS), "Client main thread should have started.");
        Thread.sleep(200); 

        // --- Then ---
        // 1. Verify interactions with the mocked network streams during login
        verify(mockSocketOut, times(1)).writeUTF("testuser");     // Client should send login
        verify(mockSocketOut, times(1)).writeUTF("password123");  // Client should send password

        
        // 2. Verify console output
        String consoleOutput = outputStreamCaptor.toString();
        System.err.println("Console Output during test: \n" + consoleOutput); // Print for debugging
        assertTrue(consoleOutput.contains("Connected to mockhost:12345"), "Should show connection message.");
        assertTrue(consoleOutput.contains("Enter your login:"), "Should prompt for login.");
        assertTrue(consoleOutput.contains("Enter your password:"), "Should prompt for password.");
        assertTrue(consoleOutput.contains("Welcome, testuser!"), "Should show welcome message from server.");
        assertTrue(consoleOutput.contains("Client MainLoop started"), "Should indicate main loop started.");

        // 3. Assert the client's internal state (login field should be set)
        // Accessing private fields for testing usually requires reflection or making it package-private.
        // For simplicity here, if `login` is a private field, you might need a getter or accept this limitation.
        // If login is accessible: assertEquals("testuser", client.getLogin());

        // 4. Trigger shutdown and verify threads terminate
        client.initiateShutDown(); // Sets the running flag to false for the main loop and child threads
        clientMainThread.interrupt(); // <<<--- ADD THIS LINE! Unblocks clientOutputQueue.take()
       
        // Wait for the client's main loop thread to finish its cleanup
        clientMainThread.join(2000); 
        assertFalse(clientMainThread.isAlive(), "Client main thread should have terminated.");

        // Verify that socket resources were closed
        verify(mockSocket, atLeastOnce()).close();
        verify(mockSocketIn, atLeastOnce()).close();
        verify(mockSocketOut, atLeastOnce()).close();
        
        // Optional: Verify TerminalListener and Receiver threads are also shut down
        // (This would require making them accessible or using spies on their stopRunning methods)
    }

    @Test
    @DisplayName("Should handle login failure after multiple attempts")
    void shouldHandleLoginFailure() throws Exception {
        // --- Given ---
        // Server always rejects login/password for 3 attempts
        when(mockSocketIn.readUTF())
                .thenReturn("Enter your login:")
                .thenReturn("Enter your password:") // Attempt 1
                .thenReturn("Login failed. Try again:")
                .thenReturn("Enter your password:") // Attempt 2
                .thenReturn("Login failed. Try again:")
                .thenReturn("Enter your password:") // Attempt 3
                .thenReturn("Too many failed attempts. Disconnecting."); // Final server message

        // Simulate user input for 3 failed login attempts
        String simulatedUserInput = "wronguser\nwrongpass1\nwrongpass2\nwrongpass3\n";
        System.setIn(new ByteArrayInputStream(simulatedUserInput.getBytes()));

        // --- When ---
        // Expect an IOException due to "Too many attempts" from login()
        IOException thrown = assertThrows(IOException.class, () -> {
            client = new Client(mockSocket);
        }, "Client constructor should throw IOException on too many failed login attempts.");
        
        assertEquals("Too many attempts", thrown.getMessage(), "Exception message should indicate too many attempts.");
        
        // --- Then ---
        // Verify multiple login/password attempts were sent
        verify(mockSocketOut, times(1)).writeUTF("wronguser");
        verify(mockSocketOut, times(3)).writeUTF(startsWith("wrongpass")); // Verifies all 3 password attempts

        // Verify console output
        String consoleOutput = outputStreamCaptor.toString();
        System.err.println("Console Output during login failure test: \n" + consoleOutput); // Print for debugging
        assertTrue(consoleOutput.contains("Enter your login:"), "Should prompt for login.");
        assertTrue(consoleOutput.contains("Login failed. Try again:"), "Should show login failure message.");
        // The final "Too many failed attempts" from the server is read by the client but then the client throws its own IOException
        
        // Ensure that cleanup was called implicitly or explicitly if Client constructor fails
        // In this case, since constructor throws, cleanup might not be fully executed,
        // but resources should still be closed by the JVM or AfterEach.
        verify(mockSocket, atLeastOnce()).close(); // Socket should be closed on login failure
    }

    // Add more tests for:
    // - Sending a message (TerminalListener -> clientOutputQueue -> out.writeUTF)
    // - Receiving a message (Receiver processes message, possibly to console or other internal state)
    // - Testing the `cleanup()` method's comprehensive shutdown behavior
    // - Handling InterruptedException in the main loop
    // - Handling IOException in the main loop
}