# Distributed MD5 Cracker

**A distributed, multithreaded MD5 brute-force password search system using Java RMI.**
*Developed for TMN4013 Distributed Systems Assignment 2 (2025/2026).*

## 📖 Overview
This project implements a **Distributed System** to crack MD5 password hashes. Unlike a standard standalone program, this system utilizes **Java RMI (Remote Method Invocation)** to split the heavy computational workload across **three physical machines**:
1.  **Client Node:** Manages the job, partitions the search space, and aggregates results.
2.  **Server Node 1:** Dedicates resources to searching the first half of the password space.
3.  **Server Node 2:** Dedicates resources to searching the second half of the password space.

### Key Features
* **3-PC Architecture:** Fully distributed setup verifying "True Parallelism" across physical hardware.
* **Dynamic Partitioning:** The Client automatically calculates and assigns non-overlapping search ranges (e.g., A-M to Server 1, N-Z to Server 2).
* **Remote Termination:** If one server finds the password, the Client immediately triggers a remote stop signal to all other servers to save resources.
* **Performance Logging:** Servers generate detailed `server_x.log` files tracking thread start/stop times and activity.

---

## ⚙️ Testbed Configuration
The system has been verified using a **3-PC Topology** on the following IP addresses.

### Configuration Set 1 (Primary)
| Role | Machine ID | IP Address | Function |
| :--- | :--- | :--- | :--- |
| **Client** | Machine 1 | `10.64.116.193` | Coordinator (Runs `SearchClient`) |
| **Server 1** | Machine 3 | `10.64.116.211` | Worker Node (Runs `SearchServer`) |
| **Server 2** | Machine 4 | `10.64.119.149` | Worker Node (Runs `SearchServer`) |

### Configuration Set 2 (Secondary)
| Role | Machine ID | IP Address | Function |
| :--- | :--- | :--- | :--- |
| **Client** | Machine 2 | `10.64.119.229` | Coordinator (Runs `SearchClient`) |
| **Server 1** | Machine 5 | `10.64.119.59` | Worker Node (Runs `SearchServer`) |
| **Server 2** | Machine 6 | `10.64.119.247` | Worker Node (Runs `SearchServer`) |

---

## 🚀 Execution Guide (3-PC Setup)

### Prerequisites
* Java JDK installed on all 3 machines.
* All machines must be on the same Local Area Network (LAN).
* **Firewalls must be disabled** or configured to allow traffic on port `1099`.

### Step 1: Configure Server 1 (Machine 3)
1.  Open a terminal.
2.  Start the RMI Registry:
    ```bash
    rmiregistry 1099
    ```
3.  Open a **second terminal** and start the Server application:
    ```bash
    java "-Djava.rmi.server.hostname=10.64.116.211" SearchServer Server1
    ```

### Step 2: Configure Server 2 (Machine 4)
1.  Open a terminal.
2.  Start the RMI Registry:
    ```bash
    rmiregistry 1099
    ```
3.  Open a **second terminal** and start the Server application:
    ```bash
    java "-Djava.rmi.server.hostname=10.64.119.149" SearchServer Server2
    ```

### Step 3: Run the Client (Machine 1)
1.  Open a terminal on the Client machine.
2.  Execute the client:
    ```bash
    java SearchClient
    ```
3.  Enter the configuration when prompted:
    * **MD5 Hash:** (Enter target hash)
    * **Password Length:** `2` (or target length)
    * **Number of Servers:** `2`
    * **Server 1 IP:** `10.64.116.211`
    * **Server 2 IP:** `10.64.119.149`

---

## 📊 Performance Evaluation
This setup allows for the calculation of **Speedup** and **Efficiency** metrics by comparing the execution time of the 3-PC Distributed setup against a Single-PC baseline.

* **Speedup:** $T_1 / T_n$ (Time on 1 PC / Time on 3 PCs)
* **Efficiency:** Speedup / Total Threads

---

## 👥 Group Members
* **Ng Clarence Chuan Hann** (<84832>)
* **Brendan Chan Kah Le** (<83403>)
* **<Chong Ming Zin** (<83489>)
* **Xavier Liong Zhi Hao** (<86709>)
* **Alif Aiman Bin Othman** (<83162>)
