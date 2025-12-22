# Distributed MD5 Cracker

**A distributed, multithreaded MD5 brute-force password search system using Java RMI.**
*Developed for TMN4013 Distributed Systems Assignment 2 (2025/2026).*



## 📖 Overview
This project implements a **Distributed System** to crack MD5 password hashes. [cite_start]Unlike a standard standalone program, this system utilizes **Java RMI (Remote Method Invocation)** to split the heavy computational workload across **three physical machines** per cluster [cite: 5-6, 19].

To accelerate data collection and performance evaluation, this project utilizes **two parallel 3-PC clusters**. [cite_start]This allows for simultaneous execution of different test cases to efficiently measure the system's scalability [cite: 81-87].

### Key Features
* **3-PC Architecture:** Fully distributed setup verifying "True Parallelism" across physical hardware.
* [cite_start]**Static Search-Space Partitioning:** The Client automatically calculates and assigns non-overlapping search ranges (e.g., first character '!' to 'M' for Server 1) to ensure no redundant work [cite: 60-64].
* [cite_start]**Remote Termination:** If one server finds the password, the Client immediately triggers a remote stop signal to all other servers to save resources[cite: 30, 137].
* **Real-time Progress Monitoring:** The client polls servers for progress updates and displays a real-time ETA and percentage complete.
* [cite_start]**Performance Logging:** Servers generate detailed `server_x.log` files tracking thread start/stop times and activity [cite: 70-75].

---

## ⚙️ Testbed Environments
To expedite the assignment's performance analysis, two identical distributed environments are deployed.

### 🔹 Cluster A (Configuration Set 1)
| Role | Machine ID | IP Address | Function |
| :--- | :--- | :--- | :--- |
| **Client** | Machine 1 | `10.64.116.193` | Coordinator (Runs `SearchClient`) |
| **Server 1** | Machine 3 | `10.64.116.211` | Worker Node (Runs `SearchServer`) |
| **Server 2** | Machine 4 | `10.64.119.149` | Worker Node (Runs `SearchServer`) |

### 🔹 Cluster B (Configuration Set 2)
| Role | Machine ID | IP Address | Function |
| :--- | :--- | :--- | :--- |
| **Client** | Machine 2 | `10.64.119.229` | Coordinator (Runs `SearchClient`) |
| **Server 1** | Machine 5 | `10.64.119.59` | Worker Node (Runs `SearchServer`) |
| **Server 2** | Machine 6 | `10.64.119.247` | Worker Node (Runs `SearchServer`) |

---

## 🚀 Execution Guide

### Prerequisites
* Java JDK installed on all machines.
* **Firewalls must be disabled** (or port 1099 allowed).
* **Important:** Before compiling `SearchClient.java`, you must manually update the `SERVER_1_IP` and `SERVER_2_IP` variables in the code to match the Cluster you are currently testing.

### Step 0. Compilation
Before running the system, compile all Java files from the source directory on all machines:
```bash
javac *.java

```


### Step 1: Start Server 1
Perform these steps on the designated Server 1 machine (e.g., Machine 3 or 5):

1.  **Terminal 1 (Registry):** Start the RMI registry.
    ```bash
    rmiregistry 1099
    ```
    *(Keep this window open)*

2.  **Terminal 2 (Server App):** Start the server.
    ```bash
    # Syntax: java "-Djava.rmi.server.hostname=<Current_Machine_IP>" SearchServer server_1
    # Example for Cluster A:
    java "-Djava.rmi.server.hostname=10.64.116.211" SearchServer server_1
    ```

### Step 2: Start Server 2

Open a terminal on the designated Server 2 machine (e.g., Machine 4 or 6) and run:

```bash
# Syntax: java "-Djava.rmi.server.hostname=<Current_Machine_IP>" SearchServer server_2
# Example for Cluster A:
java "-Djava.rmi.server.hostname=10.64.119.149" SearchServer server_2

```

### Step 3: Run the Client

Open a terminal on the designated Client machine (e.g., Machine 1 or 2) and run:

```bash
java SearchClient

```

Follow the on-screen prompts to input the MD5 hash, password length, threads per server, and server count .

---

## 📊 Performance Evaluation
[cite_start]This setup allows for the calculation of **Speedup** and **Efficiency** metrics by comparing the execution time of the 3-PC Distributed setup against a Single-PC baseline, as required by the assignment guidelines [cite: 90-92].

* **Speedup ($S$):**
  $$S = \frac{T_{1}}{T_{n}}$$
  *(Time on 1 Server / Time on 2 Servers)*

* **Efficiency ($E$):**
  $$E = \frac{S}{N}$$
  *(Speedup / Total Threads)*

## 👥 Group Members

* **Ng Clarence Chuan Hann** (<84832>)
* **Brendan Chan Kah Le** (<83403>)
* **Chong Ming Zin** (<83489>)
* **Xavier Liong Zhi Hao** (<86709>)
* **Alif Aiman Bin Othman** (<83162>)
