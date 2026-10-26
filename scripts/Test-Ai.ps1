param([string]$AiConfig='config\ai.local.properties',[switch]$Offline)
. (Join-Path $PSScriptRoot 'Common.ps1')
$env:AI_CONFIG_FILE=(Resolve-Path -LiteralPath $AiConfig).Path
$arguments=@('-PserverOnly=true',':server:installDist','--console=plain')
if($Offline){$arguments+='--offline'}
& (Join-Path $ProjectRoot 'gradlew.bat') @arguments
if($LASTEXITCODE -ne 0){throw 'Server build failed; live AI test was not run.'}
& (Join-Path $env:JAVA_HOME 'bin\java.exe') '-cp' (Join-Path $ProjectRoot 'server\build\install\server\lib\*') 'com.pulse.server.AiConnectionTool'
if($LASTEXITCODE -ne 0){throw 'AI access or generated-plan verification failed. See the non-secret message above.'}
