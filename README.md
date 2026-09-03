# Cyber Shadow Twin

AI-powered cybersecurity monitoring and Security Operations Center (SOC) dashboard designed to detect, analyze, visualize, and respond to simulated cyber threats.

## Overview

Cyber Shadow Twin is a cybersecurity monitoring platform that combines a web-based SOC dashboard with a Java-based backend. The system provides security event monitoring, threat detection, risk assessment, network visualization, sandbox-based attack simulation, honeypot defense, and active threat isolation.

The project also integrates Google's Gemini API for AI-assisted security analysis.

## Key Features

- 🔐 User authentication and session management
- 🛡️ Cybersecurity threat detection
- 📊 SOC-style security monitoring dashboard
- 📈 Threat and risk visualization
- 🌐 Network topology visualization
- 🧪 Isolated cybersecurity attack simulation through Sandbox
- 🍯 Honeypot-based threat detection
- 🚨 Security alerts and event logging
- 🔒 Active threat isolation
- 🔍 Forensic/security event analysis
- 🤖 Gemini-assisted threat analysis
- 📄 Security report and PDF generation
- 💾 MySQL-based data storage

## System Architecture

```text
User
 │
 ▼
Web Frontend
 │
 ├── Login
 ├── SOC Dashboard
 └── Security Sandbox
 │
 ▼
Java Backend
 │
 ├── Authentication
 ├── Threat Detection
 ├── Risk Analysis
 ├── Honeypot Defense
 ├── Threat Isolation
 └── AI Security Analysis
 │
 ├───────────────┐
 ▼               ▼
MySQL         Gemini API
Database      AI Analysis
```
## Project Structure

```text
cyber-shadow-twin/
│
├── backend/
│   ├── Main.java
│   ├── ShadowTwinUI.java
│   ├── ResetDB.java
│   ├── ai_refactor.py
│   ├── defense_refactor.py
│   ├── refactor.py
│   └── lib/
│       ├── mysql-connector-j-8.4.0.jar
│       └── protobuf-java-3.25.1.jar
│
├── dashboard.html
├── dashboard.js
├── index.html
├── login.js
├── sandbox.html
├── style.css
├── upguard-grid.js
│
└── README.md
```
Technologies Used
Frontend
HTML5
CSS3
JavaScript
D3.js / network visualization components
Backend
Java
Java HTTP Server
REST-style API endpoints
Server-Sent Events (SSE)
Database
MySQL
MySQL Connector/J
AI
Google Gemini API
Development Tools
IntelliJ IDEA
Visual Studio Code
MySQL Workbench
Security Components
Threat Detection

The system analyzes incoming security-related activity and identifies suspicious or potentially malicious behavior.

Sandbox

The Sandbox provides an isolated environment for simulating security events and observing how the monitoring and response mechanisms behave.

Honeypot

The platform includes a honeypot mechanism designed to detect unauthorized access attempts against protected resources and generate security alerts.

Active Threat Isolation

Detected threats can be isolated through the system's response mechanism, allowing the administrator to contain a simulated attacker and generate forensic information.

Risk Assessment

Security events contribute to the platform's threat/risk monitoring and visualization system, helping administrators identify potentially dangerous activity.

AI-Assisted Analysis

Cyber Shadow Twin integrates the Google Gemini API for AI-assisted security analysis.

The AI component can analyze security-related input and provide contextual threat assessments while the backend handles the application's security logic, database operations, and response mechanisms.

API keys and database credentials are intentionally excluded from this repository.

Database

The application uses MySQL for persistent storage of application and security-related information.

Database credentials are stored locally through configuration and are not included in this repository.

Running the Project
1. Configure MySQL

Create the required MySQL database and configure the local database credentials in the application's configuration file.

2. Configure Gemini

Provide a valid Gemini API key in the local configuration.

3. Start the Java Backend

Open the backend project in IntelliJ IDEA and run the appropriate Java entry point.

4. Start the Frontend

Open the frontend files using a local web server such as VS Code Live Server.

5. Access the Application

Open the frontend login page in a browser and authenticate using the configured application account.

Security Notice

This repository does not contain production credentials, API keys, database passwords, or private configuration files.

Before deploying the system publicly, replace all local development credentials with secure environment-based configuration.

Project Status

This project was developed as a cybersecurity minor project demonstrating:

Cybersecurity monitoring
Threat detection
Security event analysis
AI-assisted security analysis
Honeypot defense
Threat containment
Network visualization
Security reporting
Author

Muggiji412

Cyber Shadow Twin — AI-Powered Cybersecurity Monitoring Platform


### Step 2 — Commit it

At the bottom:

**Commit changes**

Use:

```text
Improve project README
