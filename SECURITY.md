# Security Policy

## Overview

Singularity Vault is designed as an offline-first password and sensitive data storage application.  
All security decisions prioritize data confidentiality and user control over convenience.

This document outlines the security assumptions, guarantees, and limitations of the application.

---

## Security Guarantees

Singularity Vault provides the following guarantees under its threat model:

- All stored data is encrypted locally on the device
- Encryption keys are derived from a user-controlled master password
- The master password is never stored or transmitted
- No sensitive data is sent to external servers
- Encryption keys exist in memory only during an active session
- Biometric authentication is handled exclusively by the Android OS

---

## Cryptographic Design

- Key Derivation: PBKDF2WithHmacSHA256
- Encryption: AES (256-bit)
- Randomness: SecureRandom
- Storage: Encrypted payloads stored in Room database

Salts are generated using a cryptographically secure random number generator and stored alongside encrypted data where required.

---

## Authentication Model

Singularity Vault uses a single root of trust: the master password.

- Biometrics are optional and act only as a convenience layer
- Biometric authentication never replaces the master password
- Failed biometric authentication falls back to password-based unlock

There is no password recovery or reset mechanism.

---

## Session Security

- Successful authentication creates an in-memory session
- Encryption keys are cleared from memory when the session ends
- Sessions end when the app is backgrounded, closed, or the device is locked (configurable)

This reduces exposure in the event of device compromise while the app is not actively in use.

---

## User Responsibility

Users are responsible for:
- Remembering their master password
- Choosing strong passwords when storing entries
- Securing their device against unauthorized access

If the master password is lost or the app is uninstalled, stored data cannot be recovered.

---

## Reporting Security Issues

If you discover a security vulnerability, please report it responsibly.

- Do not disclose the issue publicly
- Provide steps to reproduce where possible
- Include affected versions and expected behavior

Security reports can be submitted via the project repository or direct contact with the maintainers.

---

## Scope Limitations

Singularity Vault does not protect against:
- A compromised operating system
- Rooted or maliciously modified devices
- Physical access to an unlocked device
- Users voluntarily disclosing their own passwords

These scenarios are considered out of scope.
