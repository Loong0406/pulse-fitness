param([switch]$Offline)
. (Join-Path $PSScriptRoot 'Common.ps1')
$arguments = @(':core:test',':server:test',':app:lintDebug',':app:assembleDebug',':server:installDist','--console=plain')
if ($Offline) { $arguments += '--offline' }
& (Join-Path $ProjectRoot 'gradlew.bat') @arguments
if ($LASTEXITCODE -ne 0) { throw 'Build or validation failed. See output above.' }
New-Item -ItemType Directory -Path (Join-Path $ProjectRoot 'deliverables') -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $ProjectRoot 'app\build\outputs\apk\debug\app-debug.apk') -Destination (Join-Path $ProjectRoot 'deliverables\PulseFitness-debug.apk') -Force
Write-Output 'Ready: deliverables\PulseFitness-debug.apk'
