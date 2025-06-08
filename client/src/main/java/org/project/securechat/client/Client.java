package org.project.securechat.client;

import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
  private static final String SERVER_HOST = "localhost";
  private static final int SERVER_PORT = 12345;
  private static final Logger LOGGER = LogManager.getLogger();

  private BlockingQueue<String> serverInputQueue = new LinkedBlockingDeque<>(10);// takes server messages
  private BlockingQueue<String> clientOutputQueue = new LinkedBlockingDeque<>(10);// takes client messages
  static BlockingQueue<String> status = new LinkedBlockingQueue<>();
  private Socket socket;
  ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  private DataOutputStream out;
  private DataInputStream in;
  private BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8));
  static public String login;

  public void start() {
    try {
      socket = new Socket(SERVER_HOST, SERVER_PORT);
      out = new DataOutputStream(socket.getOutputStream());
      in = new DataInputStream(socket.getInputStream());

      ClientSender cSender = new ClientSender(out, clientOutputQueue, executor);

      ClientReceiver cReceiver = new ClientReceiver(in, serverInputQueue, clientOutputQueue, executor);

      executor.submit(cSender);
      executor.submit(cReceiver);
      // logowanie

      while (!executor.isTerminated()) {

        String messageForServer = userInput.readLine();
        if (messageForServer.equals("/exit")) {
          executor.shutdownNow();
          userInput.close();
          break;
        }
        clientOutputQueue.put(messageForServer);
        Thread.sleep(500);
        String response = Client.status.poll();
        LOGGER.info("Status {}", response);
        if (response != null && response.equals("OK")) {
          LOGGER.info("LOGOWANIE UDANE");
          ClientListener cListener = new ClientListener(clientOutputQueue, executor, userInput);
          executor.submit(cListener);

          // userInput.close();
          while (!executor.isTerminated())
            ;
          break;

        }
      }

      // LOGGER.info("logowanie nie udane");
      try {
        socket.close();
        in.close();
        out.close();
      } catch (IOException d) {
        LOGGER.error("CANNOT CLOSE STREAMS", d);
      }
    } catch (Exception e) {
      e.printStackTrace();
      try {
        socket.close();
        in.close();
        out.close();
      } catch (IOException g) {
        e.printStackTrace();
      }

    }

  }

  public static void main(String[] args) {
    Client client = new Client();
    client.start();
  }
}
