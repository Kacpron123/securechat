package org.project.securechat.sharedClass;

/**
 * Hello world!
 *
 */
public class Message
{
    public Message(String data){
      this.data = data;
    }
    public String data;
    public void write(){
      System.out.println(data);
    }
}
