# Setting Up Android Studio on Windows with GitHub SSH

This note documents a clean Windows workflow for Android Studio, PowerShell, and GitHub SSH authentication.

## Goal

Use **PowerShell on Windows** as the main terminal for Android development while keeping GitHub SSH authentication working.

## When this applies

This guide is useful when:

- the project has been moved from WSL to the Windows filesystem
- Android Studio is running on Windows
- Git operations should work from PowerShell instead of WSL
- GitHub SSH authentication needs to be configured on Windows

## Common issues

- `ssh-add` works, but `ssh -T git@github.com` returns `Permission denied (publickey)`
- `gh` is not installed in Windows PowerShell
- `ssh-agent` needs to be started from an elevated PowerShell session
- a private key was exposed in terminal output and should be rotated immediately

## Recommended setup

### 1) Create an SSH key in PowerShell

```powershell
ssh-keygen -t ed25519 -C "your_email@example.com"
```

Use the default save path:

```text
C:\Users\<windows-user>\.ssh\id_ed25519
```

A passphrase is recommended, but optional.

### 2) Start and load `ssh-agent`

If you have admin rights, open **PowerShell as Administrator** and run:

```powershell
Set-Service -Name ssh-agent -StartupType Automatic
Start-Service ssh-agent
```

Then in normal PowerShell:

```powershell
ssh-add $env:USERPROFILE\.ssh\id_ed25519
```

If `Set-Service` fails with access denied, PowerShell was not elevated.

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

## Security note

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

## Optional: Install Git Town on Windows

If your team uses Git Town, install it in PowerShell:

```powershell
winget install git-town.git-town
# or
choco install git-town
# or
scoop install git-town
```

Verify installation:

```powershell
git-town --version
```

Use Git Town from the repository root:

```powershell
git-town sync
```

## Clone the repository into Windows

Once SSH works, clone into the Windows filesystem:

```powershell
mkdir C:\dev -Force
cd C:\dev
git clone git@github.com:<owner>/<repo>.git
cd <repo>
```

## Android Studio notes

If the project is being used in Android Studio on Windows:

- Open the repository from the Windows path
- Use PowerShell as the terminal
- Set the terminal shell path to PowerShell if needed

Example shell path:

```text
C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
```

Or PowerShell 7 if installed:

```text
pwsh.exe
```

## Quick troubleshooting

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

Install GitHub CLI with `winget`, or add the SSH key through GitHub’s website.

### `git-town : The term 'git-town' is not recognized`

Install Git Town using the commands in the **Optional: Install Git Town on Windows** section above.

If it is installed but still not recognized, start a new PowerShell session and run:

```powershell
Get-Command git-town -All
git-town --version
```

