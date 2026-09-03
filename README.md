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
