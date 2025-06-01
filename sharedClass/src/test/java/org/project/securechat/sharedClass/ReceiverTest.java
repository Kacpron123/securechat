package org.project.securechat.sharedClass;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.invocation.InvocationOnMock; // Import for InvocationOnMock
import org.mockito.stubbing.Answer; // Import for Answer

import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch; // Import
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
    @Mock // Added this mock for shouldHandleInterruptedExceptionOnPut
    private BlockingQueue<Message> mockOutputQueue; // Added this mock

    private BlockingQueue<Message> outputQueue; // Keep this for tests not using the mock queue
    private Receiver receiver;

    @BeforeEach
    void setUp() {
        // Initialize for tests that use a real queue (like shouldReceiveAndProcessMessage)
        outputQueue = new LinkedBlockingQueue<Message>(5);
        // Initialize receiver with the real queue by default,
        // or re-initialize in specific tests for the mock queue.
        receiver = new Receiver(mockIn, outputQueue, mockProcessingFunction);
    }

    @Test
    @DisplayName("Should receive and process a message successfully")
    void shouldReceiveAndProcessMessage() throws Exception {
        // Given
        String jsonMessage = "{ \"senderID\" : \"user1\", \"chatID\" : \"chat1\",\"timestamp\":\"2025-05-26T08:45:48.641588900Z\",\"messageType\":\"TEXT\", \"data\":\"witam\" }";
        Message processedMessage = new Message("user1", "chat1", Message.MessageTYPE.TEXT, "Processed Hello");

        when(mockIn.readUTF())
                .thenReturn(jsonMessage) // Simulate reading one message
                .thenThrow(new IOException("Stop reading")); // Stop the loop after one message

        // This is the crucial part: The latch that the test thread will wait on.
        CountDownLatch messagePutLatch = new CountDownLatch(1);

        // Make mockProcessingFunction trigger the latch after it's called and returns
        when(mockProcessingFunction.apply(any(Message.class)))
                .thenAnswer(new Answer<Message>() {
                    @Override
                    public Message answer(InvocationOnMock invocation) throws Throwable {
                        // The actual processing logic would happen here in a real scenario
                        // For the mock, we return our predefined processedMessage
                        messagePutLatch.countDown(); // Signal that the processing and put attempt is done
                        return processedMessage;
                    }
                });
        // Alternatively, using a lambda for thenAnswer (more concise for Java 8+)
        // when(mockProcessingFunction.apply(any(Message.class)))
        //         .thenAnswer(invocation -> {
        //             messagePutLatch.countDown();
        //             return processedMessage;
        //         });


        // When
        Thread receiverThread = new Thread(receiver);
        receiverThread.start();

        // Wait for the latch to count down, meaning the message was processed and put
        // This makes the test wait for the actual event, not just a fixed time.
        assertTrue(messagePutLatch.await(5, TimeUnit.SECONDS), "Message should have been processed and put into the queue within timeout");

        // The thread might still be running and attempting to readUTF() again,
        // which will hit the IOException defined in `when(mockIn.readUTF())`.
        // We can safely stop it here.
        receiver.stopRunning();
        receiverThread.interrupt(); // Interrupt to unblock DataInputStream.readUTF() if blocked
        receiverThread.join(2000); // Wait for the thread to finish

        // Then
        verify(mockIn, times(2)).readUTF(); // Ensure readUTF was called exactly once before throwing
        verify(mockProcessingFunction, times(1)).apply(any(Message.class)); // Ensure processing function was called

        Message receivedFromQueue = outputQueue.poll(2, TimeUnit.SECONDS); // Try to retrieve from queue
        assertNotNull(receivedFromQueue, "Message should be in the queue");
        assertEquals(processedMessage.getData(), receivedFromQueue.getData());
        assertEquals(processedMessage.getMessageType(), receivedFromQueue.getMessageType());
        // Add more assertions for other fields if necessary
    }

    // Your shouldHandleIOException test (no change needed here)
    @Test
    @DisplayName("Should handle IOException during message reception")
    void shouldHandleIOException() throws Exception {
        // Given
        when(mockIn.readUTF())
                .thenThrow(new IOException("Simulated network error"));

        // When
        Thread receiverThread = new Thread(receiver);
        receiverThread.start();
        Thread.sleep(100); // Give it time to hit the exception
        receiver.stopRunning();
        receiverThread.interrupt();
        receiverThread.join(1000);

        // Then
        verify(mockIn, atLeastOnce()).readUTF(); // It tries to read
        verify(mockProcessingFunction, never()).apply(any()); // No message to process
        assertTrue(outputQueue.isEmpty(), "Queue should be empty on IOException");
        // We can't directly assert System.out.println output without a Capture output utility
    }

    @Test
    @DisplayName("Should handle InterruptedException when putting to queue")
    void shouldHandleInterruptedExceptionOnPut() throws Exception {
        // Line A: Sets the 'receiver' field of the test class to a new Receiver using mockOutputQueue
        receiver = new Receiver(mockIn, mockOutputQueue, mockProcessingFunction); 
       
        String jsonMessage = "{ \"senderID\" : \"Adam\", \"chatID\" : \"Pawel\",\"timestamp\":\"2025-05-26T08:45:48.641588900Z\",\"messageType\":\"TEXT\", \"data\":\"bardzo wazna wiadomosc\" }";
        Message processedMessage = new Message("user1", "chat1", Message.MessageTYPE.TEXT, "Processed Hello");
        
        // This is a duplicate line (Line B), but it doesn't change the outcome of the test's setup.
        // It just reassigns the 'receiver' field to an identical new instance.
        // The mock setups that follow refer to the @Mock fields, not this local 'receiver' variable itself.
        // remove this line: receiver = new Receiver(mockIn, mockOutputQueue, mockProcessingFunction); 

        when(mockIn.readUTF())
                .thenReturn(jsonMessage); 

        when(mockProcessingFunction.apply(any(Message.class)))
                .thenReturn(processedMessage); 

        // This mock applies to the 'mockOutputQueue' field, which is correctly passed to the 'receiver' at Line A.
        doThrow(new InterruptedException("Simulated queue put interruption"))
                .when(mockOutputQueue)
                .put(any(Message.class));

        // When
        // The 'receiver' field here *is* the one created at Line A (or Line B if it existed and was the last).
        // It's the one using mockOutputQueue.
        Thread receiverThread = new Thread(receiver); 
        receiverThread.start();

        // Give the thread time to execute
        Thread.sleep(1000);

        // Then (expectations)
        verify(mockIn, times(1)).readUTF(); 
        verify(mockProcessingFunction, times(1)).apply(any(Message.class)); // This is still failing
        verify(mockOutputQueue, times(1)).put(processedMessage); 

        receiverThread.join(1000);
        assertFalse(receiverThread.isAlive(), "Receiver thread should have terminated due to InterruptedException");
    }

    @Test
    @DisplayName("stopRunning should set the running flag to false")
    void stopRunningShouldSetFlag() {
        // Given
        // running is true by default
        // When
        receiver.stopRunning();
        // Then
        assertFalse(receiver.running.get(), "Running flag should be false after stopRunning");
    }
}