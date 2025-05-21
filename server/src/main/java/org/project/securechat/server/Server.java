package org.project.securechat.server;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.*;

public class Server {
   private static final Logger LOGGER = LogManager.getLogger(); 

  private String name;

  public static void main(String[]args){
    System.out.println("server on");
  }
}
