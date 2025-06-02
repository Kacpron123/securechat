package org.project.securechat.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.securechat.sharedClass.Message;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference; // Not strictly needed for this test class

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerminalListenerTest {

    @Mock
    private Scanner mockScanner;
    @Mock
    private BlockingQueue<Message> mockOutputQueue;
    @Mock
    private Client mockClient;

    private String testUserID = "testUser";
    private TerminalListener terminalListener;

    // For capturing System.out and System.err
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        // Redirect System.out and System.err to capture their output
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));

        // Initialize TerminalListener with mocks.
        // It now defaults chatID to "Server" in its constructor.
        terminalListener = new TerminalListener(mockScanner, mockOutputQueue, testUserID, mockClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Restore original System.out and System.err
        System.setOut(originalOut);
        System.setErr(originalErr);
        outContent.close();
        errContent.close();

        // Clean up temporary file if created
        if (tempFilePath != null && Files.exists(tempFilePath)) {
            Files.delete(tempFilePath);
        }
    }

    @Test
    @DisplayName("Constructor should initialize fields correctly")
    void constructorInitializesFields() {
        assertNotNull(terminalListener);
        assertTrue(terminalListener.running.get(), "Running flag should be true initially");
        // No direct assertion for private fields like chatID unless getter is available
        // but we can infer it from behavior in other tests.
    }

    @Test
    @DisplayName("stopRunning should set the running flag to false")
    void stopRunningShouldSetFlag() {
        terminalListener.stopRunning();
        assertFalse(terminalListener.running.get(), "running flag should be false after stopRunning");
    }

    @Test
    @DisplayName("getFirstArgument should extract the first argument correctly")
    void getFirstArgument_shouldExtractCorrectly() {
        assertEquals("argument", terminalListener.getFirstArgument("/command argument"));
        assertEquals("long argument with spaces", terminalListener.getFirstArgument("/command long argument with spaces"));
        assertEquals("123", terminalListener.getFirstArgument("/command 123"));
        assertEquals("padded", terminalListener.getFirstArgument("/command   padded  ")); // getFirstArgument trims it
        assertNull(terminalListener.getFirstArgument("/command     ")); // getFirstArgument trims it
    }

    @Test
    @DisplayName("getFirstArgument should return null if no argument")
    void getFirstArgument_shouldReturnNullIfNoArgument() {
        assertNull(terminalListener.getFirstArgument("/command"));
        assertNull(terminalListener.getFirstArgument("/command "));
        assertNull(terminalListener.getFirstArgument("/command     "));
    }

    @Test
    @DisplayName("processing /exit command should initiate shutdown and return exit message")
    void processing_exitCommand() throws IOException, InterruptedException {
        // Given that chatID defaults to "Server", this test will pass fine.
        Message result = terminalListener.processing("/exit");

        // Verify client.initiateShutDown() was called
        verify(mockClient, times(1)).initiateShutDown();

        // Verify the returned Message
        assertNotNull(result);
        assertEquals(testUserID, result.getSenderID());
        // chatID will be "Server" as per constructor default
        assertEquals("Server", result.getChatID());
        assertEquals(Message.MessageTYPE.COMMAND, result.getMessageType());
        assertEquals("/exit", result.getData());

        verifyNoInteractions(mockOutputQueue);
    }

    @Test
    @DisplayName("processing /chat command should set chatID and return null")
    void processing_chatCommand() throws IOException {
        String newChatID = "newChatRoom";
        Message result = terminalListener.processing("/chat " + newChatID);

        assertNull(result, "Processing /chat command should return null");

        // Verify a message can now be sent to the new chatID
        String messageToSend = "Hello there!";
        Message textMessage = terminalListener.processing(messageToSend);
        assertNotNull(textMessage);
        assertEquals(testUserID, textMessage.getSenderID());
        assertEquals(newChatID, textMessage.getChatID());
        assertEquals(Message.MessageTYPE.TEXT, textMessage.getMessageType());
        assertEquals(messageToSend, textMessage.getData());

        // Verify output feedback
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Joined chat: " + newChatID), "Should print chat join confirmation");
    }

    @Test
    @DisplayName("processing /chat command with no argument should print usage and return null")
    void processing_chatCommandNoArgument() throws IOException {
        // TerminalListener starts with chatID="Server"
        Message result = terminalListener.processing("/chat");

        assertNull(result, "Processing /chat command with no argument should return null");
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Server chat"));

        // Verify chatID remains unchanged (still "Server")
        // Try to send a regular message, it should return null and print feedback
        Message regularMessageResult = terminalListener.processing("Test message");
        assertNull(regularMessageResult);
    }

    @Test
    @DisplayName("processing /leave command should set chatID to 'Server' and return null")
    void processing_leaveCommand() throws IOException {
        // First set to a non-server chatID
        terminalListener.processing("/chat someChat");
        outContent.reset(); // Clear previous output

        Message result = terminalListener.processing("/leave");

        assertNull(result, "Processing /leave command should return null");

        // Verify chatID is set to "Server" by attempting to send a regular message
        Message regularMessageResult = terminalListener.processing("regular message");
        assertNull(regularMessageResult);
        assertTrue(outContent.toString().contains("Server chat"), "Should confirm leaving chat");

        verifyNoInteractions(mockOutputQueue);
    }

    @Test
    @DisplayName("processing /leave command when already in 'Server' chat should print feedback and return null")
    void processing_leaveCommandAlreadyInServer() throws IOException {
        // TerminalListener starts with chatID="Server"
        Message result = terminalListener.processing("/leave");

        assertNull(result, "Processing /leave command should return null");
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Server chat"));

        verifyNoInteractions(mockOutputQueue);
    }

    // --- Test /file command (more complex, needs temporary file handling) ---
    private Path tempFilePath;

    private Path createTempFile(String content, String extension) throws IOException {
        tempFilePath = Files.createTempFile("testFile", "." + extension);
        Files.write(tempFilePath, content.getBytes(StandardCharsets.UTF_8));
        return tempFilePath;
    }

    @Test
    @Disabled("TODO files")
    @DisplayName("processing /file command should send file content as MESSAGE_TYPE.FILE")
    void processing_fileCommand() throws IOException {
        String fileContent = "This is a test file content.";
        String fileExtension = "txt";
        Path testFile = createTempFile(fileContent, fileExtension);

        // Ensure a chatID is set for the message
        terminalListener.processing("/chat fileTestChat");
        outContent.reset(); // Clear output from /chat command

        Message result = terminalListener.processing("/file " + testFile.toAbsolutePath().toString());

        assertNotNull(result, "Processing /file command should return a message");
        assertEquals(testUserID, result.getSenderID()); // NOW EXPECTS testUserID
        assertEquals("fileTestChat", result.getChatID());
        assertEquals(Message.MessageTYPE.FILE, result.getMessageType());
        assertEquals(fileExtension + " " + fileContent, result.getData(), "File content should be in data field");

        verifyNoInteractions(mockOutputQueue);
    }
    
    @Test
    @Disabled("TODO files")
    @DisplayName("processing /file command with no extension should default to 'bin'")
    void processing_fileCommandNoExtension() throws IOException {
        String fileContent = "binary data";
        Path testFile = Files.createTempFile("noExtension", ""); // Create file without extension
        Files.write(testFile, fileContent.getBytes(StandardCharsets.UTF_8));
        tempFilePath = testFile; // Ensure it's cleaned up

        terminalListener.processing("/chat binaryChat");
        outContent.reset();

        Message result = terminalListener.processing("/file " + testFile.toAbsolutePath().toString());

        assertNotNull(result);
        assertEquals(testUserID, result.getSenderID());
        assertEquals("binaryChat", result.getChatID());
        assertEquals(Message.MessageTYPE.FILE, result.getMessageType());
        assertTrue(result.getData().startsWith("bin "), "Should default to 'bin' extension");
        assertTrue(result.getData().contains(fileContent), "File content should be present");
    }

    @Test
    @Disabled("TODO files")
    @DisplayName("processing /file command should handle file not found IOException")
    void processing_fileCommandFileNotFound() throws IOException {
        Path nonExistentFile = Paths.get("nonExistentFile.txt");
        // Ensure chatID is set to avoid null pointer in processing of regular messages later
        terminalListener.processing("/chat dummyChat");
        outContent.reset(); // clear previous output

        Message result = terminalListener.processing("/file " + nonExistentFile.toAbsolutePath().toString());

        assertNull(result, "Processing /file command with non-existent file should return null");
        String consoleErrorOutput = errContent.toString(); // Check System.err
        assertTrue(consoleErrorOutput.contains("Error: File not found or not readable: " + nonExistentFile.toAbsolutePath().toString()),
                   "System.err should contain the error message for file not found");
        verifyNoInteractions(mockOutputQueue);
    }
    
    @Test
    @Disabled("TODO files")
    @DisplayName("processing /file command with no argument should print usage and return null")
    void processing_fileCommandNoArgument() throws IOException {
        Message result = terminalListener.processing("/file");

        assertNull(result, "Processing /file command with no argument should return null");
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Usage: /file <file_path>"), "Should print usage message");
    }


    @Test
    @DisplayName("processing unknown command should print 'Unknown command' and return null")
    void processing_unknownCommand() throws IOException {
        Message result = terminalListener.processing("/unknownCmd");

        assertNull(result, "Processing unknown command should return null");
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Unknown command"), "Should print 'Unknown command'");
        verifyNoInteractions(mockOutputQueue);
    }

    @Test
    @DisplayName("processing regular message should return TEXT message if chatID is set and not 'Server'")
    void processing_regularMessageInChat() throws IOException {
        terminalListener.processing("/chat activeChat"); // Set chatID
        outContent.reset(); // Clear output from /chat command

        String messageText = "Hello, this is a regular message.";
        Message result = terminalListener.processing(messageText);

        assertNotNull(result, "Processing regular message should return a Message");
        assertEquals(testUserID, result.getSenderID());
        assertEquals("activeChat", result.getChatID());
        assertEquals(Message.MessageTYPE.TEXT, result.getMessageType());
        assertEquals(messageText, result.getData());
        verifyNoInteractions(mockOutputQueue);
    }

    @Test
    @DisplayName("processing regular message should return null and print feedback if chatID is 'Server'")
    void processing_regularMessageInServerChat() throws IOException {
        // TerminalListener starts with chatID="Server" by default
        String messageText = "Hello, this is a regular message.";
        Message result = terminalListener.processing(messageText);

        assertNull(result, "Processing regular message in Server chat should return null");
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Please join a chat first"), "Should print feedback to join chat");
        verifyNoInteractions(mockOutputQueue);
    }

    // --- Test run() method ---

    @Test
    @DisplayName("run method should read input, process, and put valid messages to queue, then terminate on /exit")
    void run_shouldProcessAndPutMessagesAndTerminateOnExit() throws Exception {
        // Given
        // TerminalListener starts in "Server" chat.
        // Simulate scanner input: change chat, send message, send file, then exit
        when(mockScanner.nextLine())
                .thenReturn("/chat myTestChat")         // Changes chatID, returns null from processing
                .thenReturn("First message to chat")    // Regular message
                .thenReturn("/exit");                   // Terminates run loop

        // When
        Thread listenerThread = new Thread(terminalListener);
        listenerThread.start();

        // Give it time to process all commands and then the exit command
        listenerThread.join(4000); // Give generous time
        assertFalse(listenerThread.isAlive(), "TerminalListener thread should have terminated.");

        // Then
        // Verify nextLine was called for all inputs
        verify(mockScanner, times(3)).nextLine();

        // Verify the regular message was put to the queue
        verify(mockOutputQueue, times(1)).put(argThat(msg ->
                msg.getSenderID().equals(testUserID) &&
                msg.getChatID().equals("myTestChat") &&
                msg.getMessageType() == Message.MessageTYPE.TEXT &&
                msg.getData().equals("First message to chat")
        ));
        // Verify the /exit message was also put to the queue
        verify(mockOutputQueue, times(1)).put(argThat(msg ->
            msg.getMessageType() == Message.MessageTYPE.COMMAND &&
            msg.getData().equals("/exit")
        ));

        // Verify client.initiateShutDown() was called due to /exit
        verify(mockClient, times(1)).initiateShutDown();
        assertFalse(terminalListener.running.get(), "Running flag should be false after /exit command");

    }

    @Test
    @DisplayName("run method should handle InterruptedException during queue put and terminate")
    void run_shouldHandleInterruptedExceptionOnPut() throws Exception {
        // Given
        terminalListener.processing("/chat myTestChat"); // Set chatID
        outContent.reset(); // Clear previous output

        // Simulate scanner input: one message that will cause an interruption on put
        // when(mockScanner.hasNextLine()).thenReturn(true); // First true
        when(mockScanner.nextLine()).thenReturn("Message to cause interruption");


        // Configure mockOutputQueue to throw InterruptedException on put
        doThrow(new InterruptedException("Simulated queue put interruption"))
                .when(mockOutputQueue)
                .put(any(Message.class));

        // When
        Thread listenerThread = new Thread(terminalListener);
        listenerThread.start();

        // Give it time to process the line and hit the interruption
        listenerThread.join(2000); // Max wait for termination
        assertFalse(listenerThread.isAlive(), "TerminalListener thread should have terminated.");

        // Then
        verify(mockScanner, times(1)).nextLine(); // Only one line processed before put error
        verify(mockOutputQueue, times(1)).put(any(Message.class)); // put was attempted once

        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("TerminalListener interrupted [Shutting down]: Simulated queue put interruption"));
        assertTrue(consoleOutput.contains("TerminalListener finished."));
        assertFalse(terminalListener.running.get(), "Running flag should be false after interruption");

        // Verify no shutdown initiated if only an InterruptedException from put occurs (no /exit command)
        verifyNoInteractions(mockClient);
    }

    @Test
    @Disabled("TODO file sending")
    @DisplayName("run method should handle IOException during file reading but continue running")
    void run_shouldHandleIOExceptionOnFileReadAndContinue() throws Exception {
        // Given
        // Simulate a non-existent file path for the /file command
        Path nonExistentFile = Paths.get("nonExistentFileForRunTest.txt");
        
        when(mockScanner.hasNextLine()).thenReturn(true);
        when(mockScanner.nextLine())
                .thenReturn("/file " + nonExistentFile.toAbsolutePath().toString()) // This will cause IOException in processing()
                .thenReturn("regular message after error") // Listener should continue to process this
                .thenReturn("/exit"); // Then exit

        terminalListener.processing("/chat continueChat"); // Set chatID for the regular message
        outContent.reset(); // Clear setup output

        // When
        Thread listenerThread = new Thread(terminalListener);
        listenerThread.start();

        // Give it time to process all commands
        listenerThread.join(3000); // Max wait for termination
        assertFalse(listenerThread.isAlive(), "TerminalListener thread should have terminated.");

        // Then
        verify(mockScanner, times(3)).nextLine(); // /file, regular msg, /exit
        
        String consoleErrorOutput = errContent.toString(); // Check System.err
        assertTrue(consoleErrorOutput.contains("TerminalListener file/IO error: Error: File not found or not readable: " + nonExistentFile.toAbsolutePath().toString()),
                   "System.err should contain the IOException message for file read.");
        
        // The regular message after the error should still be processed and put
        verify(mockOutputQueue, times(1)).put(argThat(msg ->
            msg.getMessageType() == Message.MessageTYPE.TEXT &&
            msg.getData().equals("regular message after error")
        ));
        
        // The /exit message should also be put
        verify(mockOutputQueue, times(1)).put(argThat(msg ->
            msg.getMessageType() == Message.MessageTYPE.COMMAND &&
            msg.getData().equals("/exit")
        ));
        
        // Verify client.initiateShutDown() was called due to /exit
        verify(mockClient, times(1)).initiateShutDown();
        assertFalse(terminalListener.running.get(), "Running flag should be false after /exit command");

        String consoleOutput = outContent.toString();
        assertFalse(consoleOutput.contains("TerminalListener input stream closed or exhausted"), "Listener should not stop due to single IOException");
        assertTrue(consoleOutput.contains("TerminalListener finished."));
    }

    @Test
    @DisplayName("run method should not put null messages to queue, and print feedback")
    void run_shouldNotPutNullMessages() throws Exception {
        // Given
        when(mockScanner.nextLine())
                .thenReturn("/chat newChat") // Returns null, changes chatID
                .thenReturn("/leave")       // Returns null, changes chatID to Server
                .thenReturn("regular message") // Returns null because chatID is Server, prints feedback
                .thenReturn("/unknownCommand") // Returns null, prints unknown command feedback
                .thenReturn("/exit");       // To terminate the loop

        // When
        Thread listenerThread = new Thread(terminalListener);
        listenerThread.start();

        listenerThread.join(3000);
        assertFalse(listenerThread.isAlive(), "TerminalListener thread should have terminated.");

        // Then
        verify(mockScanner, times(5)).nextLine();
        
        // Only the /exit message should have been put to the queue
        verify(mockOutputQueue, times(1)).put(argThat(msg -> msg.getMessageType() == Message.MessageTYPE.COMMAND));
        verifyNoMoreInteractions(mockOutputQueue); // No other puts

        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Joined chat: newChat"), "Should confirm joining chat.");
        assertTrue(consoleOutput.contains("Server chat"), "Should confirm leaving chat.");
        assertTrue(consoleOutput.contains("Please join a chat first"), "Should advise to join chat for regular message.");
        assertTrue(consoleOutput.contains("Unknown command"), "Should print 'Unknown command' with command.");
        assertTrue(consoleOutput.contains("TerminalListener finished."), "Should print finished message.");

        String consoleErrorOutput = errContent.toString();
        assertFalse(consoleErrorOutput.contains("Error"), "Should not print errors for valid null returns.");
    }
    
    @Test
    @DisplayName("run method should terminate if scanner input stream closes (NoSuchElementException)")
    void run_shouldTerminateOnScannerClosed() throws Exception {
        // Given
        // Simulate nextLine throwing NoSuchElementException on the first call
        when(mockScanner.nextLine())
                .thenThrow(new java.util.NoSuchElementException("Simulated scanner closed"));

        // When
        Thread listenerThread = new Thread(terminalListener);
        listenerThread.start();

        listenerThread.join(2000); // Give time to terminate
        assertFalse(listenerThread.isAlive(), "TerminalListener thread should have terminated due to scanner closing.");

        // Then
        verify(mockScanner, atLeastOnce()).nextLine(); // Or times(1) depending on exact mock behavior
        assertFalse(terminalListener.running.get(), "Running flag should be false after scanner exhaustion/closure.");
        verifyNoInteractions(mockOutputQueue);
        verifyNoInteractions(mockClient);

        String consoleErrorOutput = errContent.toString();
        assertTrue(consoleErrorOutput.contains("TerminalListener input stream closed or exhausted: Simulated scanner closed"),
                   "System.err should contain the input stream exhaustion message.");
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("TerminalListener finished."), "Should print finished message.");
    }
}