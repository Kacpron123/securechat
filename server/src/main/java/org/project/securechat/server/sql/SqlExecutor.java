package org.project.securechat.server.sql;
import org.project.securechat.sharedClass.Message;
import org.project.securechat.sharedClass.Message.DataType;
public class SqlExecutor implements Runnable{
  
  private Message message;
  public SqlExecutor(Message message){
    this.message = message;
  }
  @Override
  public void run(){
    if(message.getDataType().equals(DataType.TEXT)){
      if(message.getChatID()!= null){
          SqlHandlerMessages.insertMessage(message.getSenderID(), message.getChatID(), message.getData(), message.getTimestamp().toString());
      }
    
    }
  }
  
}
