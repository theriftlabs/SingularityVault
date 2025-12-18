# Encryption Key Lifecycle

## Key Creation

- The user sets a master password during setup
- A cryptographically secure random salt is generated
- PBKDF2 derives a 256-bit encryption key
- The derived key is never written to disk

---

## Key Usage

- The derived key is used to encrypt and decrypt vault entries
- All encryption and decryption happens locally
- Plaintext data exists only in memory during active use

---

## Session Scope

- Keys are kept in memory only during an authenticated session
- Sessions are created after successful password or biometric authentication
- Keys are cleared when the session ends

---

## Key Destruction

Keys are explicitly wiped from memory when:
- The app moves to the background
- The app is closed
- The device is locked (configurable)

This minimizes the exposure window in case of device compromise.
