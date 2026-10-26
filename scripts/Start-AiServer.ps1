param([string]$AiConfig='config\ai.local.properties',[string]$DatabaseConfig='config\database.local.properties',[int]$Port=8080,[switch]$Offline)
$parameters=@{AiConfig=$AiConfig;DatabaseConfig=$DatabaseConfig;Port=$Port}
if($Offline){$parameters['Offline']=$true}
& (Join-Path $PSScriptRoot 'Start-Server.ps1') @parameters
