# Deprecated

This file has been replaced by [`docs/android-studio-windows-ssh-setup.md`](docs/android-studio-windows-ssh-setup.md), which uses a more descriptive and reusable title for the Windows + Android Studio + GitHub SSH workflow.

# Windows PowerShell GitHub SSH Setup After Moving From WSL

This note documents how to move a GitHub SSH workflow from WSL to Windows PowerShell, especially when the project is being moved to the Windows filesystem.

## Goal

Use **PowerShell on Windows** as the main terminal for Git operations while keeping GitHub SSH authentication working.

## Situation

This guide applies when GitHub authentication was working in **WSL**, but the project has moved to the **Windows filesystem** and the preferred terminal is **PowerShell**.

The main issue was:

- `ssh-add` worked
- `ssh -T git@github.com` returned:

```text
git@github.com: Permission denied (publickey).
```

Common issues include:

- `gh` was not installed in Windows PowerShell
- `ssh-agent` configuration needed Windows admin rights
- a private key was exposed in terminal output and therefore should be rotated immediately

## Recommended Setup

### 1) Create a new SSH key in PowerShell

```powershell
ssh-keygen -t ed25519 -C "your_email@example.com"
```

Use the default save path:

```text
C:\Users\<windows-user>\.ssh\id_ed25519
```

You can use a passphrase or leave it empty.

### 2) Load the key into `ssh-agent`

If you have admin rights, open **PowerShell as Administrator** and run:

```powershell
Set-Service -Name ssh-agent -StartupType Automatic
Start-Service ssh-agent
```

Then in normal PowerShell:

```powershell
ssh-add $env:USERPROFILE\.ssh\id_ed25519
```

If `Set-Service` fails with access denied, it means PowerShell is not elevated.

### 3) Add the public key to GitHub

Show the public key:

```powershell
Get-Content $env:USERPROFILE\.ssh\id_ed25519.pub
```

Copy the full line and add it in GitHub:

- `Settings`
- `SSH and GPG keys`
- `New SSH key`
- paste the key
- save

### 4) Force SSH to use that key for GitHub

Create or edit this file:

```powershell
notepad $env:USERPROFILE\.ssh\config
```

Add:

```text
Host github.com
  HostName github.com
  User git
  IdentityFile ~/.ssh/id_ed25519
  IdentitiesOnly yes
```

### 5) Test authentication

```powershell
ssh -T git@github.com
```

Expected result:

```text
Hi <github-username>! You've successfully authenticated, but GitHub does not provide shell access.
```

If it still fails, use verbose output:

```powershell
ssh -vT git@github.com
```

## Important Security Note

Do **not** print your private key to the terminal.

The private key file is:

```text
C:\Users\<windows-user>\.ssh\id_ed25519
```

The public key file is:

```text
C:\Users\<windows-user>\.ssh\id_ed25519.pub
```

If the private key is exposed, rotate it immediately:

1. Delete the old key files
2. Generate a new key
3. Add only the `.pub` file to GitHub

## Optional: Install GitHub CLI on Windows

If you want `gh` available in PowerShell:

```powershell
winget install --id GitHub.cli
```

## Clone the repository into Windows

Once SSH works, clone into the Windows filesystem:

```powershell
mkdir C:\dev -Force
cd C:\dev
git clone git@github.com:<owner>/<repo>.git
cd <repo>
```

## Android Studio Notes

If this project is being used in Android Studio on Windows:

- Open the repository from the Windows path
- Use PowerShell as the terminal
- Set terminal shell path to PowerShell if needed

Example shell path:

```text
C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
```

Or PowerShell 7 if installed:

```text
pwsh.exe
```

## Quick Troubleshooting

### `Permission denied (publickey)`

Possible causes:

- the public key was not added to GitHub
- SSH is using a different key than expected
- `~/.ssh/config` is missing or incorrect
- `ssh-agent` is not running
- the wrong GitHub account is being used

### `Set-Service : Access is denied`

Run PowerShell as Administrator.

### `gh : The term 'gh' is not recognized`

Install GitHub CLI with `winget`, or just add the SSH key through GitHub’s website.
