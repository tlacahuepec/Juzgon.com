# Security Policy

## Supported Versions

Only the latest release and the current `develop` branch receive active security updates.

| Version | Supported |
|---|---|
| Latest Release | :white_check_mark: |
| `develop` branch | :white_check_mark: |
| Older releases | :x: |

---

## Reporting a Vulnerability

We take the security of our repositories seriously. If you believe you have found a security vulnerability, please follow these steps:

1. **Do NOT open a public issue.**
2. Report the vulnerability privately to the project maintainers via email or GitHub Private Vulnerability Reporting.
3. Include:
   - Type of issue (e.g. buffer overflow, SQL injection, cross-site scripting)
   - Detailed steps to reproduce the vulnerability
   - Affected components, configurations, or versions
   - Potential impact of the vulnerability
   - Proof of concept or exploit code, if available

---

## Response Timeline & SLAs

- **Initial Response**: Within 48 hours of report receipt.
- **Triage & Confirmation**: Within 5 business days.
- **Patch Release**: Critical vulnerabilities patched within 7 business days of confirmation.

---

## Secret and Credential Handling

- We enforce a **zero-tolerance policy** for committed secrets, tokens, private keys, or passwords.
- If a secret is committed, it must be considered compromised and immediately rotated.
- Automated secret scanning is enabled on all pull requests and branch pushes.
