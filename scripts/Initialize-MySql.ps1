param([string]$AdminConfig='config\database-admin.local.properties',[switch]$Offline)
. (Join-Path $PSScriptRoot 'Common.ps1')
$adminPath=(Resolve-Path -LiteralPath $AdminConfig).Path
$appPath=Join-Path $ProjectRoot 'config\database.local.properties'
$arguments=@('-PserverOnly=true',':server:installDist','--console=plain')
if($Offline){$arguments+='--offline'}
& (Join-Path $ProjectRoot 'gradlew.bat') @arguments
if($LASTEXITCODE -ne 0){throw 'Server build failed; MySQL initialization was not run.'}
& (Join-Path $env:JAVA_HOME 'bin\java.exe') '-cp' (Join-Path $ProjectRoot 'server\build\install\server\lib\*') 'com.pulse.server.BootstrapMysql' $adminPath $appPath
if($LASTEXITCODE -ne 0){throw 'MySQL initialization did not complete. See the non-secret error above.'}
