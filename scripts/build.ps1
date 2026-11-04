[CmdletBinding()]
param([switch]$WindowsTrust)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$previousOptions = $env:MAVEN_OPTS
Push-Location $projectRoot
try {
    $maven = Get-Command mvn -ErrorAction SilentlyContinue
    $mavenPath = if ($maven) { $maven.Source } else {
        Get-ChildItem (Join-Path $projectRoot '.runtime/apache-maven-*/bin/mvn.cmd') -File |
            Select-Object -First 1 -ExpandProperty FullName
    }
    if (!$mavenPath) { throw 'Maven nao encontrado no PATH nem em .runtime/apache-maven-*/bin.' }
    if ($WindowsTrust) {
        # Usa somente as autoridades ja confiaveis do usuario Windows.
        # Nao modifica cacerts, registro, proxy ou variaveis permanentes.
        $env:MAVEN_OPTS = "$previousOptions -Djavax.net.ssl.trustStore=NONE -Djavax.net.ssl.trustStoreType=Windows-ROOT"
    }
    & $mavenPath -B -ntp clean package
    if ($LASTEXITCODE -ne 0) { throw 'A compilacao Maven falhou. Consulte a saida acima.' }
}
finally {
    $env:MAVEN_OPTS = $previousOptions
    Pop-Location
}
