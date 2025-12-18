# Threat Model

## Purpose

This document describes the threat model for Singularity Vault and clarifies which risks the application is designed to mitigate and which are explicitly out of scope.

---

## Assets

The primary assets protected by Singularity Vault are:

- Stored passwords and sensitive entries
- Encryption keys derived from the master password
- User trust in local-only data handling

---

## Threat Actors

The following threat actors are considered:

- Opportunistic attackers with access to the device
- Malicious applications attempting local data access
- Curious users attempting to bypass safeguards
- Accidental exposure due to poor password choices

---

## In-Scope Threats and Mitigations

### 1. Unauthorized App Access

**Threat:**  
An attacker attempts to access stored data without authentication.

**Mitigation:**  
- Master password–based encryption
- Biometric authentication as an optional layer
- Encrypted storage with no plaintext data on disk

---

### 2. Database Extraction

**Threat:**  
An attacker copies the local database files from the device.

**Mitigation:**  
- All sensitive data is encrypted
- No encryption keys are stored alongside the database
- Extracted data is unusable without the master password

---

### 3. Weak User Passwords

**Threat:**  
Users store weak or predictable passwords.

**Mitigation:**  
- Local password strength analysis
- Clear warnings for weak passwords
- Secure password suggestions

Final responsibility remains with the user.

---

### 4. Shoulder Surfing and Accidental Exposure

**Threat:**  
Passwords are exposed while viewing entries.

**Mitigation:**  
- Explicit user actions required to reveal sensitive fields
- Session timeouts reduce exposure window

---

## Out-of-Scope Threats

Singularity Vault does not attempt to mitigate:

- A fully compromised or rooted operating system
- Malware with elevated system privileges
- Physical access to an already unlocked device
- Users intentionally sharing their credentials
- Hardware-level attacks

These scenarios are outside the security guarantees of the application.

---

## Design Tradeoffs

Certain features were intentionally excluded:

- Cloud synchronization increases attack surface
- Password recovery weakens cryptographic guarantees
- Online accounts introduce identity and data risks

Singularity Vault favors explicit user responsibility over implicit trust models.

---

## Assumptions

The security model assumes:
- The Android OS enforces application sandboxing correctly
- Cryptographic primitives are implemented correctly by the platform
- Users understand the implications of losing their master password
