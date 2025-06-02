package org.project.securechat.sharedClass;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream; // For capturing System.out
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream; // For restoring System.out
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiverTest {

    @Mock
    private DataInputStream mockIn;
    @Mock
    private Function<Message, Message> mockProcessingFunction;
    @Mock
    private BlockingQueue<Message> mockOutputQueue;
    @Mock
    private ShutdownSignal mockShutdown;

    private BlockingQueue<Message> realOutputQueue; // For tests using a real queue

    private Receiver receiver;

    // For capturing System.out
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        // Redirect System.out before each test
        System.setOut(new PrintStream(outContent));

        // Initialize a real queue for tests that need it
        realOutputQueue = new LinkedBlockingQueue<>(5);

        // Initialize receiver with the correct constructor signature
        // Using realOutputQueue by default; specific tests can re-initialize with mockOutputQueue
        receiver = new Receiver(mockIn, realOutputQueue, mockProcessingFunction, mockShutdown);
    }

    @AfterEach
    void tearDown() {
        // Restore original System.out after each test
        System.setOut(originalOut);
        try {
            outContent.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Should receive and process a message successfully and put to queue")
    void shouldReceiveAndProcessMessage() throws Exception {
        // Given
        String jsonMessage = "{ \"senderID\" : \"user1\", \"chatID\" : \"chat1\",\"timestamp\":\"2025-05-26T08:45:48.641588900Z\",\"messageType\":\"TEXT\", \"data\":\"witam\" }";
        Message processedMessage = new Message("user1", "chat1", Message.MessageTYPE.TEXT, "Processed Hello");

        // Simulate reading one message, then throw IOException to ensure the finally block is hit
        when(mockIn.readUTF())
                .thenReturn(jsonMessage)
                .thenThrow(new IOException("Simulated stream end for test cleanup"));

        // Make mockProcessingFunction return the processed message
        when(mockProcessingFunction.apply(any(Message.class)))
                .thenReturn(processedMessage);

        // When
        Thread receiverThread = new Thread(receiver);
        receiverThread.start();

        // Wait for the message to appear in the queue
        Message receivedFromQueue = realOutputQueue.poll(2, TimeUnit.SECONDS);

        // Then
        assertNotNull(receivedFromQueue, "Message should be in the queue");
        assertEquals(processedMessage.getData(), receivedFromQueue.getData());
        assertEquals(processedMessage.getMessageType(), receivedFromQueue.getMessageType());

        // Verify that readUTF was called exactly twice (once for the message, then once for the exception)
        verify(mockIn, times(2)).readUTF();
        // Verify that the processing function was called exactly once
        verify(mockProcessingFunction, times(1)).apply(any(Message.class));

        // Ensure the receiver thread eventually terminates (due to IOException from mockIn)
        receiver.stopRunning(); // Signal to stop
        receiverThread.interrupt(); // Interrupt to unblock DataInputStream.readUTF() if still blocked
        receiverThread.join(2000); // Wait for the thread to finish
        assertFalse(receiverThread.isAlive(), "Receiver thread should have terminated.");

        // Now, with cleanup() in finally, it will ALWAYS be called when the run() method exits,
        // even if it's due to our simulated IOException.
        verify(mockShutdown, times(1)).cleanup();
    }

    @Test
    @DisplayName("Should handle IOException during message reception and trigger shutdown cleanup")
    void shouldHandleIOException() throws Exception {
        // Given
        // Make mockIn.readUTF() throw the exception immediately on the first call
        when(mockIn.readUTF())
                .thenThrow(new IOException("Simulated network error"));

        // When
        Thread receiverThread = new Thread(receiver);
        receiverThread.start();

        // Give it a moment to hit the exception and exit.
        // No need for a long sleep, as the exception is immediate.
        Thread.sleep(50); // Small delay to let the thread start and hit the mock

        // Then
        // Verify readUTF was called at least once (it should be exactly once as it immediately throws)
        verify(mockIn, atLeastOnce()).readUTF();
        verify(mockProcessingFunction, never()).apply(any());

        // Crucial: Wait for the thread to actually finish before asserting its state
        receiverThread.join(2000); // Wait for the thread to terminate (2 seconds max)

        // Now assert the running flag and thread status
        assertFalse(receiver.running.get(), "Receiver running flag should be false after IOException");
        assertFalse(receiverThread.isAlive(), "Receiver thread should have terminated due to IOException");
        
        verify(mockShutdown, times(1)).cleanup();

        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Error receiving message:"),
                   "Console output should contain the error message.");
    }

    @Test
    @DisplayName("Should handle InterruptedException during message reception and trigger shutdown cleanup")
    void shouldHandleInterruptedExceptionOnRead() throws Exception {
        // Given
        when(mockIn.readUTF()).thenAnswer(invocation -> {
            Thread.sleep(Long.MAX_VALUE); // Simulate infinite blocking
            return null;
        });

        // When
        Thread receiverThread = new Thread(receiver);
        receiverThread.start();

        Thread.sleep(100);
        receiverThread.interrupt();

        // Then
        receiverThread.join(2000);
        verify(mockIn, atLeastOnce()).readUTF();
        verify(mockProcessingFunction, never()).apply(any());
        assertFalse(receiver.running.get(), "Receiver running flag should be false after InterruptedException");
        assertFalse(receiverThread.isAlive(), "Receiver thread should have terminated due to InterruptedException");

        verify(mockShutdown, times(1)).cleanup(); // Cleanup called due to InterruptedException in finally block

        assertTrue(outContent.toString().contains("Receiver interrupted [Shutting down]:"),
                   "Console output should contain interruption message.");
    }

    @Test
    @DisplayName("Should handle InterruptedException when putting message to queue and trigger shutdown cleanup")
    void shouldHandleInterruptedExceptionOnPut() throws Exception {
        // Given
        receiver = new Receiver(mockIn, mockOutputQueue, mockProcessingFunction, mockShutdown);

        String jsonMessage = "{ \"senderID\" : \"Adam\", \"chatID\" : \"Pawel\",\"timestamp\":\"2025-05-26T08:45:48.641588900Z\",\"messageType\":\"TEXT\", \"data\":\"bardzo wazna wiadomosc\" }";
        Message processedMessage = new Message("user1", "chat1", Message.MessageTYPE.TEXT, "Processed Hello");

        when(mockIn.readUTF())
                .thenReturn(jsonMessage)
                .thenAnswer(invocation -> {
                    Thread.sleep(Long.MAX_VALUE);
                    return null;
                });

        when(mockProcessingFunction.apply(any(Message.class)))
                .thenReturn(processedMessage);

        doThrow(new InterruptedException("Simulated queue put interruption"))
                .when(mockOutputQueue)
                .put(any(Message.class));

        // When
        Thread receiverThread = new Thread(receiver);
        receiverThread.start();

        Thread.sleep(500);

        // Then
        verify(mockIn, times(1)).readUTF();
        verify(mockProcessingFunction, times(1)).apply(any(Message.class));
        verify(mockOutputQueue, times(1)).put(processedMessage);

        assertFalse(receiver.running.get(), "Receiver running flag should be false after InterruptedException on put");
        verify(mockShutdown, times(1)).cleanup(); // Cleanup called due to InterruptedException in finally block
        receiverThread.join(2000);
        assertFalse(receiverThread.isAlive(), "Receiver thread should have terminated due to InterruptedException on put");

        assertTrue(outContent.toString().contains("Receiver interrupted [Shutting down]:"),
                   "Console output should contain interruption message from put.");
    }

    @Test
    @DisplayName("stopRunning should set the running flag to false")
    void stopRunningShouldSetFlag() {
        // Given
        // When
        receiver.stopRunning();
        // Then
        assertFalse(receiver.running.get(), "Running flag should be false after stopRunning");
    }
}