# Distributed MD5 Cracker

**A distributed, multithreaded MD5 brute-force password search system using Java RMI.**
*Developed for TMN4013 Distributed Systems Assignment 2 (2025/2026).*

## 👥 Group Members

* **Ng Clarence Chuan Hann** (<84832>)
* **Brendan Chan Kah Le** (<83403>)
* **Chong Ming Zin** (<83489>)
* **Xavier Liong Zhi Hao** (<86709>)
* **Alif Aiman Bin Othman** (<83162>)
---

## 📖 Overview
This project implements a **Distributed System** to crack MD5 password hashes. Unlike a standard standalone program, this system utilizes **Java RMI (Remote Method Invocation)** to split the heavy computational workload across **three physical machines** per cluster.

To accelerate data collection and performance evaluation, this project utilizes **two parallel 3-PC clusters**. This allows for simultaneous execution of different test cases to efficiently measure the system's scalability.

### Key Features
* **3-PC Architecture:** Fully distributed setup verifying "True Parallelism" across physical hardware.
* **Static Search-Space Partitioning:** The Client automatically calculates and assigns non-overlapping search ranges (e.g., first character '!' to 'M' for Server 1) to ensure no redundant work.
* **Remote Termination:** If one server finds the password, the Client immediately triggers a remote stop signal to all other servers to save resources.
* **Real-time Progress Monitoring:** The client polls servers for progress updates and displays a real-time ETA and percentage complete.
* **Performance Logging:** Servers generate detailed `server_x.log` files tracking thread start/stop times and activity.

---

## ⚙️ Testbed Environments
To expedite the assignment's performance analysis, two identical distributed environments are deployed.

## 🚀 Execution Guide

### Prerequisites
* Java JDK installed on all machines.
* **Firewalls must be disabled** (or port 1099 allowed).
* **Important:** Before compiling `SearchClient.java`, you must manually update the `SERVER_1_IP` and `SERVER_2_IP` variables in the code to match the Cluster you are currently testing.

### Step 0. Compilation
First, replace the ip address of machine 1 and machine 2 in the code SearchClient.java (line 37 & 38) to match the Cluster you are currently testing. 
Then, compile all Java files from the source directory on all machines:
```bash
javac *.java

```


### Step 1: Start Server 1
Perform these steps on the designated Server 1 machine:

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
Perform these steps on the designated Server 2 machine:

1.  **Terminal 1 (Registry):** Start the RMI registry.
    ```bash
    rmiregistry 1099
    ```
    *(Keep this window open)*

2.  **Terminal 2 (Server App):** Start the server.
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
This setup allows for the calculation of **Speedup** and **Efficiency** metrics by comparing the execution time of the 3-PC Distributed setup against a Single-PC baseline, as required by the assignment guidelines.

* **Speedup ($S$):**
  $$S = \frac{T_{1}}{T_{n}}$$
  *(Time on 1 Server / Time on 2 Servers)*

* **Efficiency ($E$):**
  $$E = \frac{S}{N}$$
  *(Speedup / Total Threads)*

---
