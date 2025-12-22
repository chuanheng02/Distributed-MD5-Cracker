Here is a professionally structured and enhanced `README.md` for your project. I have organized your specific network configurations into a clear "Testbed Environment" section and refined the execution commands for clarity.

You can copy and paste the markdown code below directly into your GitHub repository.

---

# Distributed MD5 Cracker

**A distributed, multithreaded MD5 brute-force password search system using Java RMI.** *Developed for TMN4013 Distributed Systems Assignment 2 (2025/2026).*

## 📖 Overview

This project transforms a standalone brute-force password cracker into a **Distributed System**. By utilizing **Java RMI (Remote Method Invocation)**, the application offloads the computational workload across multiple machines (servers). This parallel processing significantly reduces the time required to crack MD5 hashes compared to a single-machine approach.

### Key Features

* **Distributed Architecture:** Supports 1 Client and up to 2 Servers.
* **Search Space Partitioning:** Automatically divides the ASCII search space between available servers to prevent overlapping work.
* **Multithreading:** Each server further divides its assigned range among local threads.
* **Real-time Reporting:** Returns the cracked password, time taken, and the specific thread ID that found the solution.

---

## ⚙️ Network Configuration & Testbed

The system has been verified on the following network topology using distinct physical/virtual machines.

### Configuration Set A (Primary Test)

| Role | Machine ID | IP Address | Description |
| --- | --- | --- | --- |
| **Client** | Machine 1 | `10.64.116.193` | Manages the search & aggregates results |
| **Server 1** | Machine 3 | `10.64.116.211` | Worker node (Range 1) |
| **Server 2** | Machine 4 | `10.64.119.149` | Worker node (Range 2) |

### Configuration Set B (Secondary Test)

| Role | Machine ID | IP Address | Description |
| --- | --- | --- | --- |
| **Client** | Machine 2 | `10.64.119.229` | Manages the search & aggregates results |
| **Server 1** | Machine 5 | `10.64.119.59` | Worker node (Range 1) |
| **Server 2** | Machine 6 | `10.64.119.247` | Worker node (Range 2) |

---

## 🚀 Installation & Usage

### 1. Compilation

Ensure you have the Java JDK installed on all machines. Navigate to the source folder and compile the code:

```bash
javac *.java

```

### 2. Running the Servers

Perform these steps on **each** Server machine (e.g., Machines 3, 4, 5, 6).

**Step A: Start the RMI Registry**
Open a terminal and run:

```bash
rmiregistry 1099

```

*(Keep this terminal window open/running)*

**Step B: Start the Search Server**
Open a **new** terminal window and execute the following. Replace `<server_ip>` with the machine's actual IP and `<ServerName>` with `Server1` or `Server2`.

```bash
# Syntax
java "-Djava.rmi.server.hostname=<server_ip>" SearchServer <ServerName>

# Example for Machine 3 (Server 1)
java "-Djava.rmi.server.hostname=10.64.116.211" SearchServer Server1

# Example for Machine 4 (Server 2)
java "-Djava.rmi.server.hostname=10.64.119.149" SearchServer Server2

```

> **Note:** The server name must be exactly `Server1` or `Server2` as the Client is programmed to look for these specific names.

### 3. Running the Client

Perform these steps on the Client machine (e.g., Machine 1 or 2).

```bash
java SearchClient

```

Follow the on-screen prompts:

1. **MD5 Hash:** Enter the target hash.
2. **Length:** Password length (e.g., `2`).
3. **Servers:** Enter `2`.
4. **IP Addresses:** Enter the IPs of the servers you started (e.g., `10.64.116.211` for Server 1).

---

## 🔧 Troubleshooting

* **Connection Refused:** Ensure the firewall on the Server machines allows traffic on port `1099`.
* **"Connection refused to host: 127.0.0.1":** This means the server was started without the `-Djava.rmi.server.hostname` flag. Restart the server with its public IP.
* **ClassNotFoundException:** Ensure all machines have the compiled `.class` files (specifically `SearchService.class` interface).
