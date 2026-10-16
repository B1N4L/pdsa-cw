# AMOffline - installer for Windows
#
# Installs AMOffline for the current user (no administrator needed): copies it to
# %LOCALAPPDATA%\Programs\AMOffline, adds Start menu and desktop shortcuts, and lists it in
# Settings > Apps so it can be uninstalled. Java is included.
#
# The shortcuts start AMOffline with the signed Java runtime that comes with it (javaw.exe),
# so Windows Smart App Control does not block it.
#
# Run it from the extracted AMOffline-Windows.zip ("Install AMOffline.cmd"), or online in PowerShell:
#     irm __AMS_BASE_URL__/install.ps1 | iex

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$Version = '__AMS_VERSION__'
$Base = if ($env:AMS_BASE_URL) { $env:AMS_BASE_URL } else { '__AMS_BASE_URL__' }
# These can be changed for testing; normal installs use the defaults
$Target = if ($env:AMS_INSTALL_DIR) { $env:AMS_INSTALL_DIR } else { Join-Path $env:LOCALAPPDATA 'Programs\AMOffline' }
$ShortcutDirs = if ($env:AMS_SHORTCUT_DIR) { @($env:AMS_SHORTCUT_DIR) } else {
    @([Environment]::GetFolderPath('Programs'), [Environment]::GetFolderPath('Desktop'))
}
$UninstallKey = if ($env:AMS_REG_KEY) { $env:AMS_REG_KEY } else { 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\AMOffline' }

function Step($text) { Write-Host "  $text" }

Write-Host ''
Write-Host "Installing AMOffline $Version" -ForegroundColor Cyan
Write-Host ''

# 1. Find the app files: next to this script (extracted zip), or download them
$cleanup = $null
$scriptDir = $PSScriptRoot
if ($scriptDir -and (Test-Path (Join-Path $scriptDir '..\runtime\bin\javaw.exe'))) {
    $source = (Resolve-Path (Join-Path $scriptDir '..')).Path
} else {
    $zipUrl = "$Base/downloads/AMOffline-Windows.zip"
    Step "Downloading $zipUrl ..."
    [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
    $cleanup = Join-Path ([IO.Path]::GetTempPath()) ('amoffline-install-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $cleanup | Out-Null
    $zip = Join-Path $cleanup 'AMOffline-Windows.zip'
    (New-Object Net.WebClient).DownloadFile($zipUrl, $zip)
    Step 'Unpacking ...'
    Expand-Archive -Path $zip -DestinationPath $cleanup
    $source = Join-Path $cleanup 'AMOffline'
}
if (-not (Test-Path (Join-Path $source 'runtime\bin\javaw.exe')) -or -not (Test-Path (Join-Path $source 'app\AMOffline.jar'))) {
    throw "The AMOffline files were not found in $source"
}

try {
    # 2. Close AMOffline if it is running from the install folder, then replace the old files
    Get-Process javaw, java -ErrorAction SilentlyContinue |
        Where-Object { $_.Path -and $_.Path.StartsWith($Target, [StringComparison]::OrdinalIgnoreCase) } |
        Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 500
    Step "Copying files to $Target ..."
    New-Item -ItemType Directory -Force -Path $Target | Out-Null
    foreach ($old in 'runtime', 'app') {
        $path = Join-Path $Target $old
        if (Test-Path $path) { Remove-Item -Recurse -Force -LiteralPath $path }
    }
    Copy-Item -Recurse -Force (Join-Path $source 'runtime') $Target
    Copy-Item -Recurse -Force (Join-Path $source 'app') $Target
    Copy-Item -Force (Join-Path $source 'amoffline.ico') $Target
    Copy-Item -Force (Join-Path $source 'setup\uninstall.ps1') $Target

    # 3. Start menu and desktop shortcuts that run AMOffline with the included (signed) Java
    $javaw = Join-Path $Target 'runtime\bin\javaw.exe'
    $jar = Join-Path $Target 'app\AMOffline.jar'
    $shell = New-Object -ComObject WScript.Shell
    $made = @()
    foreach ($dir in $ShortcutDirs) {
        if (-not $dir) { continue }
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
        $path = Join-Path $dir 'AMOffline.lnk'
        $link = $shell.CreateShortcut($path)
        $link.TargetPath = $javaw
        $link.Arguments = "-jar `"$jar`""
        $link.WorkingDirectory = $Target
        $link.IconLocation = (Join-Path $Target 'amoffline.ico') + ',0'
        $link.Description = 'Organise appointments offline'
        $link.Save()
        $made += $path
    }
    Step 'Added shortcuts to the Start menu and desktop'

    # 4. List it in Settings > Apps, with an uninstaller
    $sizeKb = [int]((Get-ChildItem -Recurse -File $Target | Measure-Object -Property Length -Sum).Sum / 1024)
    $uninstall = "powershell.exe -NoProfile -ExecutionPolicy Bypass -File `"$(Join-Path $Target 'uninstall.ps1')`""
    New-Item -Force -Path $UninstallKey | Out-Null
    $values = @{
        DisplayName = 'AMOffline'; DisplayVersion = $Version; Publisher = 'Trio Branch'
        DisplayIcon = (Join-Path $Target 'amoffline.ico'); InstallLocation = $Target; URLInfoAbout = 'https://triobranch.com'
        UninstallString = $uninstall; QuietUninstallString = "$uninstall -Quiet"
    }
    foreach ($name in $values.Keys) { Set-ItemProperty -Path $UninstallKey -Name $name -Value $values[$name] }
    foreach ($name in 'NoModify', 'NoRepair') { New-ItemProperty -Force -Path $UninstallKey -Name $name -Value 1 -PropertyType DWord | Out-Null }
    New-ItemProperty -Force -Path $UninstallKey -Name EstimatedSize -Value $sizeKb -PropertyType DWord | Out-Null

    # remember what was added, for the uninstaller
    $info = @("key=$UninstallKey") + ($made | ForEach-Object { "shortcut=$_" })
    Set-Content -Path (Join-Path $Target 'install-info.txt') -Value $info -Encoding UTF8
    Step 'Listed in Settings > Apps (for uninstalling)'
} finally {
    if ($cleanup) { Remove-Item -Recurse -Force -LiteralPath $cleanup -ErrorAction SilentlyContinue }
}

Write-Host ''
Write-Host 'AMOffline is installed. Open it from the Start menu or the desktop shortcut.' -ForegroundColor Green
Write-Host "Your appointments are saved in $(Join-Path $env:USERPROFILE 'AMOffline')"
Write-Host ''

# 5. Start AMOffline now
if (-not $env:AMS_NO_LAUNCH) {
    Start-Process -FilePath $javaw -ArgumentList "-jar `"$jar`"" -WorkingDirectory $Target
}
