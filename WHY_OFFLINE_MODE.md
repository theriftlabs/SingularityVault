# Why Singularity Vault Is Offline-Only

Singularity Vault is intentionally designed as an offline-only application.

This decision is not a limitation, but a deliberate security tradeoff.

---

## Reduced Attack Surface

Any form of cloud synchronization introduces additional risks:

- Server-side breaches
- Account takeover attacks
- API vulnerabilities
- Misconfigured access controls
- Trust dependencies on third-party infrastructure

By operating entirely offline, Singularity Vault eliminates these risks at their source.

---

## Clear Ownership Model

In Singularity Vault:
- Data exists only on the user’s device
- Encryption keys are derived only from the user’s master password
- No external system can access or recover user data

This creates a clear and honest ownership model.  
The user is fully responsible for their data, and the application does not pretend otherwise.

---

## No Implicit Trust

Many password managers require users to trust:
- Remote servers
- Account recovery mechanisms
- Synchronization logic
- Identity verification processes

Singularity Vault avoids implicit trust models entirely.  
If access is granted, it is because the user authenticated locally and correctly.

---

## Tradeoffs

Choosing an offline-only design means accepting limitations:

- No cross-device sync
- No automatic backups
- No password recovery

These tradeoffs are intentional and clearly communicated.

Singularity Vault prioritizes cryptographic integrity over convenience.

---

## Intended Audience

Singularity Vault is designed for users who:
- Prefer control over automation
- Understand the consequences of strong encryption
- Value transparency over feature density

It is not intended to replace cloud-based password managers for users who prioritize convenience.
