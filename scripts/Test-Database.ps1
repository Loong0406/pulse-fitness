param([string]$DatabaseConfig='config\database.local.properties',[switch]$InitializeTables,[switch]$Offline)
. (Join-Path $PSScriptRoot 'Common.ps1')
$arguments=@('-PserverOnly=true',':server:installDist','--console=plain')
if($Offline){$arguments+='--offline'}
& (Join-Path $ProjectRoot 'gradlew.bat') @arguments
if($LASTEXITCODE -ne 0){throw 'Server build failed.'}
$env:DB_CONFIG_FILE=(Resolve-Path -LiteralPath $DatabaseConfig).Path
$operation=if($InitializeTables){'init'}else{'check'}
& (Join-Path $env:JAVA_HOME 'bin\java.exe') '-cp' (Join-Path $ProjectRoot 'server\build\install\server\lib\*') 'com.pulse.server.DatabaseTool' $operation
if($LASTEXITCODE -ne 0){throw 'Database check failed. Credentials were not displayed.'}
