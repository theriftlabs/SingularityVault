# Singularity Vault

**Singularity Vault** is a secure, offline-first Android application for storing passwords and sensitive information.
The app is designed with a strict **security-over-convenience** philosophy: all data is encrypted locally on the device, and no information is ever transmitted to external servers.

Singularity Vault is intended for users who want full ownership and responsibility over their data.

---

## Features

* Local-only encrypted vault
* Master password–based encryption
* AES encryption with derived keys
* PBKDF2 key derivation with unique salt
* Biometric authentication (Fingerprint / Face ID)
* Session-based unlocking
* Password strength analysis
* Secure password suggestions
* Dark and light themes
* Fully offline operation
* No trackers, no analytics, no cloud sync

---

## Security Philosophy

Singularity Vault is built around a simple principle:

**Security should not be weakened for convenience.**

Unlike many password managers, Singularity Vault does not rely on cloud storage, online accounts, or recovery mechanisms. All cryptographic operations happen locally on the device, and the user retains full control over their data.

As a result:

* There is no password recovery mechanism
* There is no silent fallback if authentication fails
* There is no external access to stored data

If the master password is lost, the data cannot be recovered. This is an intentional design decision.

---

## Architecture Overview

* Platform: Android
* Language: Kotlin
* UI Framework: Jetpack Compose
* Architecture Pattern: MVVM
* Database: Room (encrypted payloads)
* Cryptography: Javax Crypto, Android Keystore
* Biometrics: Android BiometricPrompt API

Sensitive operations are isolated into dedicated repositories to reduce accidental exposure and simplify auditing.

---

## Encryption and Key Management

### Master Password Flow

1. The user sets a master password during initial setup
2. A cryptographically secure random salt is generated
3. The password and salt are processed using PBKDF2
4. The derived key is used for AES-256 encryption
5. Encrypted data is stored locally in the database

The master password itself is never stored, logged, or transmitted.

---

### Cryptographic Algorithms

| Purpose        | Algorithm               |
| -------------- | ----------------------- |
| Key Derivation | PBKDF2WithHmacSHA256    |
| Encryption     | AES (256-bit)           |
| Randomness     | SecureRandom            |
| Storage        | Encrypted blobs in Room |

---

## Biometric Authentication

Biometric authentication is provided as an optional convenience feature.

* Biometrics are used only to unlock an already derived encryption key
* The master password remains the root of trust
* Biometric data is handled entirely by the Android OS

If biometric authentication fails or is unavailable, the app safely falls back to master password authentication.

---

## Session Management

Singularity Vault uses session-based access control.

* Successful authentication creates a temporary in-memory session
* Encryption keys exist only for the duration of the session
* Sessions are terminated when:

  * The app moves to the background
  * The app is closed
  * The device is locked (configurable)

On session termination, sensitive keys are wiped from memory.

---

## Password Strength Analysis

Before storing a password, Singularity Vault evaluates its strength locally on the device.

Checks include:

* Password length
* Character diversity (uppercase, lowercase, digits, symbols)
* Repetition and predictable patterns
* Common structural weaknesses

This analysis is fully offline and does not rely on third-party services or APIs.

---

## Secure Password Suggestions

When a password is classified as weak:

* The user is clearly informed
* A strong, randomly generated password is suggested
* The user may:

  * Accept the suggested password
  * Modify it
  * Proceed with their original password

Singularity Vault does not automatically replace user input.
The final decision always remains with the user.

---

## Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/71d9c788-c024-4867-b2fa-c5ebd853c62b" width="250">
  <img src="https://github.com/user-attachments/assets/55bf781d-6eb2-4f8a-8aad-59278a7fa488" width="250">
  <img src="https://github.com/user-attachments/assets/cd7f3704-0f79-4c4c-becb-211f410c6fb0" width="250">
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/c4ec6460-0d0c-4ca4-b136-f47a1f30ba2d" width="250">
  <img src="https://github.com/user-attachments/assets/430deed2-ba20-445e-be55-68034b1fd7cc" width="250">
</p>

----

## Settings

* Enable or disable biometric authentication
* Theme selection (dark or light)
* Session timeout behavior
* Security and usage notices
* Link to official website

---

## Limitations and Non-Features

Singularity Vault intentionally does not include:

* Cloud synchronization
* Account-based login
* Password recovery or reset
* Telemetry or usage tracking
* Third-party data sharing

---

## Important Notice

If the master password is forgotten or the app is uninstalled, stored data cannot be recovered.

This behavior is intentional and aligns with the app’s security model.

---

## Setup and Build

### Requirements

* Android Studio (latest stable version)
* Android SDK 24 or higher
* Kotlin 1.9+

### Build Instructions

git clone https://github.com/theriftlabs/SingularityVault.git

Open the project in Android Studio and run it on a physical device for full biometric support.

---

## Testing Notes

* Biometric features require fingerprints or face data to be configured at the system level
* Some biometric functionality may be limited on emulators
* Cryptographic features should be tested on real hardware when possible

---

## Roadmap

* Custom password generation rules
* Password reuse detection
* Encrypted manual backups
* Advanced session control options
* Formal threat model documentation

---

## Team

Singularity Vault is developed by the Rift Labs team as a collaborative project focused on secure Android development and clean architecture.

---

## License

This project is licensed under the MIT License.

---

