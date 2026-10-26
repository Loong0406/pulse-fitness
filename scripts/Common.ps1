$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
function Use-ProjectJdk {
    $requiredVersion=21
    $criteriaFile=Join-Path $ProjectRoot 'gradle\gradle-daemon-jvm.properties'
    if ((Test-Path -LiteralPath $criteriaFile) -and ([IO.File]::ReadAllText($criteriaFile) -match '(?m)^toolchainVersion=(\d+)')) { $requiredVersion=[int]$Matches[1] }
    $candidates = @($env:JAVA_HOME)
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCommand) { $candidates += Split-Path -Parent (Split-Path -Parent $javaCommand.Source) }
    $adoptium = Join-Path $env:ProgramFiles 'Eclipse Adoptium'
    if (Test-Path -LiteralPath $adoptium) { $candidates += Get-ChildItem -LiteralPath $adoptium -Directory | Select-Object -ExpandProperty FullName }
    $candidates += @('D:\Android studio\Android studio\jbr', (Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'))
    foreach ($candidate in $candidates) {
        if (-not $candidate) { continue }
        $releaseFile = Join-Path $candidate 'release'
        if (Test-Path -LiteralPath $releaseFile) {
            $release = [IO.File]::ReadAllText($releaseFile)
            if ($release -match 'JAVA_VERSION="(\d+)') {
                $major=[int]$Matches[1]
                $matchesCriteria=if(Test-Path -LiteralPath $criteriaFile){$major -eq $requiredVersion}else{$major -ge $requiredVersion}
                if ($matchesCriteria -and (Test-Path -LiteralPath (Join-Path $candidate 'bin\java.exe'))) { $env:JAVA_HOME=$candidate; return }
            }
        }
    }
    throw "Choose an installed JDK matching version $requiredVersion or the Android Studio bundled JDK."
}
Use-ProjectJdk
Set-Location -LiteralPath $ProjectRoot
