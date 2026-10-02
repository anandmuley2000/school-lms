$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$javaHome = 'C:\Program Files\Java\jdk-21'
$requiredPorts = 8080, 5173, 8081
$windowsPowerShell = 'C:\Windows\System32\WindowsPowerShell\v1.0'

if (-not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) {
    throw "Java 21 was not found at $javaHome. Install Java 21 or edit the JAVA_HOME path in this script."
}

$nodeBin = @(
    (Join-Path $env:ProgramFiles 'nodejs'),
    (Join-Path $env:LOCALAPPDATA 'Programs\nodejs'),
    (Join-Path $env:APPDATA 'npm')
) | Where-Object { (Test-Path (Join-Path $_ 'node.exe')) -and (Test-Path (Join-Path $_ 'npm.cmd')) } | Select-Object -First 1
if (-not $nodeBin) {
    $nodeCommand = Get-Command node.exe -ErrorAction SilentlyContinue
    $npmCommand = Get-Command npm.cmd -ErrorAction SilentlyContinue
    if ($nodeCommand -and $npmCommand) { $nodeBin = Split-Path $nodeCommand.Source }
}
if (-not $nodeBin -or -not (Test-Path (Join-Path $nodeBin 'npx.cmd'))) {
    throw 'Node.js with npm and npx was not found. Install the Node.js LTS Windows package, reopen PowerShell, and run this launcher again.'
}

$busy = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $requiredPorts -contains $_.LocalPort }
if ($busy) {
    foreach ($processId in ($busy.OwningProcess | Sort-Object -Unique)) {
        $owner = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if (-not $owner -or $owner.ProcessName -notin @('java', 'node')) {
            throw "Port owner PID $processId is not a Java or Node LMS process. Close that application manually, then run this launcher again."
        }
        Write-Host "Stopping old LMS process $($owner.ProcessName) (PID $processId)."
        Stop-Process -Id $processId -Force -ErrorAction Stop
    }
    Start-Sleep -Seconds 2
    $busy = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $requiredPorts -contains $_.LocalPort }
    if ($busy) { throw 'An LMS port is still occupied after stopping the old app processes. Close the process shown by Get-NetTCPConnection and retry.' }
}

$mysql = Get-Service -Name MySQL80 -ErrorAction SilentlyContinue
if (-not $mysql) {
    throw 'The Windows service MySQL80 was not found. Install/start your local MySQL service, then run this script again.'
}
if ($mysql.Status -ne 'Running') {
    try { Start-Service -Name MySQL80 -ErrorAction Stop }
    catch { throw 'MySQL80 is stopped and Windows denied starting it. Open PowerShell as Administrator, run Start-Service MySQL80, then run this launcher again.' }
    (Get-Service -Name MySQL80).WaitForStatus('Running', [TimeSpan]::FromSeconds(30))
}

$backendScript = @'
$ErrorActionPreference = 'Stop'
$root = '__ROOT__'
Set-Location $root
$env:JAVA_HOME = '__JAVA_HOME__'
$env:Path = (Join-Path $env:JAVA_HOME 'bin') + ';__WINDOWS_POWERSHELL__;'+ $env:Path
$env:SPRING_PROFILES_ACTIVE = 'development'
$env:PORT = '8080'
$envFile = Join-Path $root 'backend\.env'
if (Test-Path $envFile) {
    foreach ($line in Get-Content $envFile) {
        if ($line -match '^\s*([^#=\s]+)\s*=\s*(.*)\s*$') {
            $key = $Matches[1]
            $value = $Matches[2].Trim().Trim('"').Trim("'")
            [Environment]::SetEnvironmentVariable($key, $value, 'Process')
        }
    }
} else { Write-Warning 'backend\.env is missing; ensure DB and JWT environment variables are set.' }
$lanAddress = Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway } | ForEach-Object { $_.IPv4Address.IPAddress } | Where-Object { $_ -and $_ -notmatch '^127\.' -and $_ -notmatch '^169\.254\.' } | Select-Object -First 1
$corsOrigins = @('http://localhost:5173', 'http://127.0.0.1:5173')
if ($env:CORS_ALLOWED_ORIGINS) { $corsOrigins += $env:CORS_ALLOWED_ORIGINS -split ',' }
if ($lanAddress) { $corsOrigins += "http://${lanAddress}:5173" }
$env:CORS_ALLOWED_ORIGINS = ($corsOrigins | ForEach-Object { $_.Trim() } | Where-Object { $_ } | Select-Object -Unique) -join ','
Write-Host "Development CORS origins configured for localhost and this PC's LAN address."
Write-Host 'Starting Spring Boot backend at http://localhost:8080'
& .\mvnw.cmd -f backend\pom.xml spring-boot:run
if ($LASTEXITCODE -ne 0) { Write-Error "Backend exited with code $LASTEXITCODE" }
'@
$adminScript = @'
$ErrorActionPreference = 'Stop'
Set-Location '__ROOT__\admin'
$env:Path = '__NODE_BIN__' + ';' + $env:Path
Write-Host 'Starting admin panel at http://localhost:5173'
& '__NPM_CMD__' run dev -- --host 0.0.0.0
'@
$mobileScript = @'
$ErrorActionPreference = 'Stop'
Set-Location '__ROOT__\mobile'
$env:Path = '__NODE_BIN__' + ';' + $env:Path
$env:EXPO_OFFLINE = '1'
Write-Host 'Starting Expo for LAN testing; scan the QR code with Expo Go.'
& '__NPX_CMD__' expo start --lan
'@

$backendScript = $backendScript.Replace('__ROOT__', $root).Replace('__JAVA_HOME__', $javaHome).Replace('__WINDOWS_POWERSHELL__', $windowsPowerShell)
$adminScript = $adminScript.Replace('__ROOT__', $root).Replace('__NODE_BIN__', $nodeBin).Replace('__NPM_CMD__', (Join-Path $nodeBin 'npm.cmd'))
$mobileScript = $mobileScript.Replace('__ROOT__', $root).Replace('__NODE_BIN__', $nodeBin).Replace('__NPX_CMD__', (Join-Path $nodeBin 'npx.cmd'))
$shell = (Get-Process -Id $PID).Path
foreach ($entry in @(@{Name='LMS Backend'; Code=$backendScript}, @{Name='LMS Admin'; Code=$adminScript}, @{Name='LMS Mobile'; Code=$mobileScript})) {
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($entry.Code))
    Start-Process -FilePath $shell -ArgumentList @('-NoExit', '-EncodedCommand', $encoded) -WindowStyle Normal
    Start-Sleep -Seconds 2
}
Write-Host 'Started backend, admin, and Expo in separate PowerShell windows.'
Write-Host 'Backend health: http://localhost:8080/actuator/health'
Write-Host 'Admin panel:    http://localhost:5173'
