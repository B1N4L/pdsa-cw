# AMS Appointment Manager - uninstaller for Windows (run from Settings > Apps)
# Removes the app, its shortcuts and its Settings > Apps entry.
# Appointments in %USERPROFILE%\AMS are kept.
param([switch]$Quiet)

$ErrorActionPreference = 'Stop'
$Target = $PSScriptRoot

# only remove a folder that really is an AMS install
if (-not (Test-Path (Join-Path $Target 'runtime\bin\javaw.exe')) -or -not (Test-Path (Join-Path $Target 'app\AMS.jar'))) {
    throw "$Target does not look like an AMS install folder, so nothing was removed."
}

# close AMS if it is running
Get-Process javaw, java -ErrorAction SilentlyContinue |
    Where-Object { $_.Path -and $_.Path.StartsWith($Target, [StringComparison]::OrdinalIgnoreCase) } |
    Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Milliseconds 500

# shortcuts and the Settings > Apps entry, as recorded by the installer
$infoFile = Join-Path $Target 'install-info.txt'
if (Test-Path $infoFile) {
    foreach ($line in Get-Content $infoFile) {
        $parts = $line -split '=', 2
        if ($parts.Count -ne 2) { continue }
        if ($parts[0] -eq 'shortcut' -and $parts[1].EndsWith('.lnk') -and (Test-Path -LiteralPath $parts[1])) {
            Remove-Item -Force -LiteralPath $parts[1]
        }
        if ($parts[0] -eq 'key' -and (Test-Path $parts[1])) {
            Remove-Item -Recurse -Force -Path $parts[1]
        }
    }
}

# the app folder itself
Set-Location ([IO.Path]::GetTempPath())
try {
    Remove-Item -Recurse -Force -LiteralPath $Target
} catch {
    # if a file is still in use, finish removing the folder a moment later
    Start-Process cmd.exe -ArgumentList "/c timeout /t 3 /nobreak >nul & rmdir /s /q `"$Target`"" -WindowStyle Hidden
}

$data = Join-Path $env:USERPROFILE 'AMS'
if (-not $Quiet) {
    Add-Type -AssemblyName PresentationFramework
    [void][System.Windows.MessageBox]::Show("AMS has been removed.`n`nYour appointments are still in $data", 'AMS Appointment Manager')
} else {
    Write-Host "AMS has been removed. Your appointments are still in $data"
}
