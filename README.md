# CollabCode-backend

CollabCode is a real-time code editor that allows multiple users to join a shared room and collaborate by writing code simultaneously. This backend is built using Java, Spring Boot, and WebSockets to enable real-time communication between clients.

## Features

- **Real-Time Collaboration**: Multiple users can join a room and see code updates as they happen.
- **WebSocket-Based Communication**: Ensures low-latency, bidirectional communication between the server and connected clients.
- **Room Management**: Users can create or join rooms with a unique room ID, and code changes within a room are synchronized across all participants.

## Technologies Used

- **Java**
- **Spring Boot**
- **WebSockets**

## Setup and Installation

### Prerequisites

- Java 17 or higher
- Maven
- A suitable IDE like IntelliJ IDEA or VS Code

### Running the Backend Locally

1. **Clone the repository:**

   ```bash
   git clone https://github.com/yourusername/CollabCode-backend.git
   cd CollabCode-backend

2. **Build the Project**

    Use Maven to build the project:

   ```bash
   mvn clean install

3. **Run the application:**

   Run the Spring Boot application:

   ```bash
   mvn spring-boot:run
   ```
   The server will start on http://localhost:8080.

## WebSocket Endpoints
The backend exposes WebSocket endpoints for real-time communication. Clients connect to these endpoints to join rooms, send code changes, and receive updates.

## Available Actions
- **JOIN**: Join a room with a specified room ID.
- **CODE_CHANGE**: Send code changes to all clients in the room.
- **LEAVE**: Leave the room


