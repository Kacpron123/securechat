package org.project.securechat.sharedClass;

public interface ShutdownSignal {
    void initiateShutDown();   
    void cleanup();
}
