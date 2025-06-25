# Secure End-to-End Java Messenger

[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://www.oracle.com/java/technologies/downloads/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## Project Overview

This project is a **secure, real-time desktop messenger built in Java** with **End-to-End (E2E) encryption**. It ensures only the sender and receiver can read messages using a hybrid RSA + AES encryption scheme. The application features secure user authentication (SHA-256 hashed passwords), real-time communication via TCP sockets and Java's virtual threads for high performance, and local SQLite databases for persistent storage of user credentials and chat histories.

---

## Installation & Setup

To get the Secure End-to-End Java Messenger up and running, follow these steps:

### Prerequisites

* **Java Development Kit (JDK) 21 or newer** is required to run the application due to its reliance on virtual threads.
* **Maven** for building the project.

### Steps

1.  **Clone the Repository:**
    First, clone the project repository to your local machine.
    ```bash
    git clone https://github.com/Kacpron123/securechat.git
    cd securechat
    ```

2.  **Build the Project with Maven:**
    Navigate to the root directory of the cloned project and use Maven to build it. This will download dependencies and compile the `.jar` files for both the server and client.
    ```bash
    mvn clean install
    ```

3.  **Run the Server:**
    Once the build is complete, you'll find the executable `.jar` files in the `target/` directory (or specific module `target/` directories if your project is multi-module).
    ```bash
    java -jar server-app.jar
    ```
    The server will start and begin listening for incoming client connections.

4.  **Run the Client(s):**
    Open a new terminal window for each client instance you wish to run.
    ```bash
    java -jar client-app.jar
    ```
    A terminal interface will appear for client. You can run multiple client instances to test the communication but remeber to place them in diffrent folders.

---