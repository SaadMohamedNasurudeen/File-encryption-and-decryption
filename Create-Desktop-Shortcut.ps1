$WshShell = New-Object -ComObject WScript.Shell
$DesktopPath = [System.Environment]::GetFolderPath('Desktop')
$ProjectDir = $PSScriptRoot
if (-not $ProjectDir) { $ProjectDir = Get-Location }

$ShortcutPath = Join-Path $DesktopPath "File Crypto Tool.lnk"
$Shortcut = $WshShell.CreateShortcut($ShortcutPath)
$Shortcut.TargetPath = Join-Path $ProjectDir "run.bat"
$Shortcut.WorkingDirectory = $ProjectDir
$Shortcut.Description = "File Encryption & Decryption System (AES-256 GCM)"
$Shortcut.WindowStyle = 7 # Minimized
$Shortcut.Save()

Write-Host "Created Desktop Shortcut at: $ShortcutPath"
