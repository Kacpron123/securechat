package org.project.securechat.client;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Scanner;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.project.securechat.sharedClass.Message;

public class TerminalListener implements Runnable{
    private BlockingQueue<Message> outputQueue;
    private final String serverChatID="Server";
    private AtomicBoolean running=new AtomicBoolean(true);
    private String chatID;
    private String userID;
    private Client client;
    Scanner scanner=new Scanner(System.in);

    //konstruktor
    public TerminalListener(Scanner scanner,BlockingQueue<Message> queue,String userID,Client client){
        this.userID=userID;
        this.outputQueue=queue;
        this.client=client;
        this.scanner=scanner;
    }

    /**
     * extracts the rest of the command
     * @param command full command string
     * @param index index where command end [TODO delete that]
     * @return rest of the command
     */
    public String getFirstArgument(String command) {
        // Trim removes leading/trailing spaces, then split by any whitespace, limiting to 2 parts.
        String[] parts = command.trim().split("\\s+", 2);
        if (parts.length > 1 && !parts[1].isEmpty()) {
            return parts[1].trim(); // Return the second part, trimmed.
        }
        return null; // No argument found or argument is empty
    }

    public void stopRunning(){
        running.set(false);
    }
    /**
     * processes the command
     * @param message command
     * @return processed message
     * @return null if command is not valid
     * @throws IOException
     */
    private Message processing(String message) throws IOException {
        if(message.startsWith("/")){
            if(message.equals("/exit")){
                running.set(false);
                client.initiateShutDown();
                return new Message(userID,chatID,Message.MessageTYPE.COMMAND,"/exit");
            }
            //starting chat
            //without arguments leave chat
            if(message.startsWith("/chat ")){
                String givenChatID=getFirstArgument(message);
                if(givenChatID==null)
                    return null;
                this.chatID=givenChatID;
                return null;
            }
            // leaving chat
            if(message.startsWith("/leave")){
                this.chatID=serverChatID;
            }
            // sending file from given path
            if(message.startsWith("/file ")){  
                Path givenPath=Paths.get(getFirstArgument(message));
                byte[] file= Files.readAllBytes(givenPath);
                String fileExtension=givenPath.toString().substring(givenPath.toString().lastIndexOf(".")+1);
                return new Message(serverChatID,chatID,Message.MessageTYPE.FILE,fileExtension+" "+new String(file,StandardCharsets.UTF_8));
            }
            // unknown command
            System.out.println("Unknown command");
            return null;
        }
        // sending message
        if(!message.startsWith("/") && !chatID.equals(serverChatID) && chatID!=null)
            return new Message(userID,chatID,Message.MessageTYPE.TEXT,message);
        
        return null;
    }
        
    
    @Override
    public void run() {
        while(running.get()){
            String message=scanner.nextLine();
            try{
                Message messageToSent=processing(message);
                if(messageToSent!=null)
                    outputQueue.put(messageToSent);;
            }catch(InterruptedException e){
                System.out.println("Error sending message: "+e.getMessage());
            }catch(IOException e){
                System.out.println("Error reading file: "+e.getMessage());
            }
        }
    }
    
}
