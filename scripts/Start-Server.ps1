param([string]$AiBaseUrl='',[string]$Model='',[int]$Port=8080,[string]$ListenAddress='127.0.0.1',[switch]$LocalOnly,[string]$DatabaseConfig='',[switch]$InitializeDatabase,[string]$AiConfig='',[switch]$Offline)
. (Join-Path $PSScriptRoot 'Common.ps1')
$arguments=@('-PserverOnly=true',':server:installDist','--console=plain')
if($Offline){$arguments+='--offline'}
& (Join-Path $ProjectRoot 'gradlew.bat') @arguments
if ($LASTEXITCODE -ne 0) { throw 'Server build failed.' }
$runtime = Join-Path $ProjectRoot '.runtime'
New-Item -ItemType Directory -Path $runtime -Force | Out-Null
if (-not $env:APP_TOKEN) {
    $tokenFile = Join-Path $runtime 'dev-token.txt'
    if (Test-Path -LiteralPath $tokenFile) { $env:APP_TOKEN = [IO.File]::ReadAllText($tokenFile).Trim() }
    else {
        $random = [Security.Cryptography.RandomNumberGenerator]::Create()
        $bytes = New-Object byte[] 24
        $random.GetBytes($bytes); $random.Dispose()
        $env:APP_TOKEN = ([BitConverter]::ToString($bytes)).Replace('-','').ToLowerInvariant()
        [IO.File]::WriteAllText($tokenFile,$env:APP_TOKEN)
    }
}
$env:HOST=$ListenAddress; $env:PORT=[string]$Port
if($PSBoundParameters.ContainsKey('Model')){$env:AI_MODEL=$Model}
if($AiConfig){$env:AI_CONFIG_FILE=(Resolve-Path -LiteralPath $AiConfig).Path}
if ($DatabaseConfig) {
    $env:DB_CONFIG_FILE=(Resolve-Path -LiteralPath $DatabaseConfig).Path
    $operation=if($InitializeDatabase){'init'}else{'check'}
    & (Join-Path $env:JAVA_HOME 'bin\java.exe') '-cp' (Join-Path $ProjectRoot 'server\build\install\server\lib\*') 'com.pulse.server.DatabaseTool' $operation
    if ($LASTEXITCODE -ne 0) { throw 'Database connection/schema check failed; server was not started.' }
} elseif ($InitializeDatabase) { throw 'Use -DatabaseConfig when initializing database tables.' }
if ($LocalOnly) { $env:AI_BASE_URL='' } elseif ($PSBoundParameters.ContainsKey('AiBaseUrl')) { $env:AI_BASE_URL=$AiBaseUrl }
Write-Output 'Copy the gateway token from .runtime\dev-token.txt into the app connection settings. Keep this terminal open; Ctrl+C stops the server.'
& (Join-Path $env:JAVA_HOME 'bin\java.exe') '-cp' (Join-Path $ProjectRoot 'server\build\install\server\lib\*') 'com.pulse.server.FitnessServer'
