[CmdletBinding()]
param([string]$TomcatHome = $env:CATALINA_HOME, [switch]$Stop, [switch]$SkipPasswordPrompt)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$base = Join-Path $projectRoot '.runtime/tomcat-base'
$war = Join-Path $projectRoot 'target/contratofacil.war'
if ($Stop) {
    # Somente o Java cuja base corresponde exatamente a este projeto.
    $pattern = '(?:^|\s)"?' + [regex]::Escape("-Dcatalina.base=$base") + '(?:"|\s|$)'
    $running = @(Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {
        $_.CommandLine -match $pattern -and $_.CommandLine.Contains('org.apache.catalina.startup.Bootstrap')
    })
    if ($running.Count -gt 1) { throw 'Mais de um Tomcat para esta base; nenhum processo foi encerrado.' }
    if ($running.Count -eq 1) { Stop-Process -Id $running[0].ProcessId }
    Write-Host 'Tomcat deste projeto parado.'
    return
}
if (!$TomcatHome) {
    $TomcatHome = Get-ChildItem (Join-Path $projectRoot '.runtime/apache-tomcat-10.1.*') -Directory |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (!$TomcatHome -or !(Test-Path (Join-Path $TomcatHome 'bin/bootstrap.jar'))) {
    throw 'Extraia o Tomcat 10.1 em .runtime ou informe -TomcatHome. Consulte o README.'
}
$TomcatHome = (Resolve-Path -LiteralPath $TomcatHome).Path
if (!(Test-Path $war)) { throw 'Execute mvn clean package primeiro.' }
foreach ($setting in @{DB_NAME='contratofacil'; DB_USER='contratofacil_app'}.GetEnumerator()) {
    $value = [Environment]::GetEnvironmentVariable($setting.Key)
    if ($value -and $value -ne $setting.Value) { throw "$($setting.Key) deve ser $($setting.Value) para este projeto." }
}
if ($env:DB_HOST -and $env:DB_HOST -notin @('localhost', '127.0.0.1')) { throw 'Use um PostgreSQL local em DB_HOST.' }
$portCheck = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 8080)
try { $portCheck.Start() } catch { throw 'Porta 8080 ocupada. Pare o servidor antes de executar novamente.' } finally { $portCheck.Stop() }

# Configuracao exclusiva do projeto; a instalacao do Tomcat nao e alterada.
foreach ($directory in @('conf', 'logs', 'temp', 'webapps', 'work')) {
    New-Item -ItemType Directory -Force -Path (Join-Path $base $directory) | Out-Null
}
foreach ($file in @('web.xml', 'catalina.properties', 'logging.properties', 'context.xml')) {
    $destination = Join-Path $base "conf/$file"
    if (!(Test-Path $destination)) { Copy-Item -LiteralPath (Join-Path $TomcatHome "conf/$file") -Destination $destination }
}
$server = Join-Path $base 'conf/server.xml'
if (!(Test-Path $server)) {
    @'
<?xml version="1.0" encoding="UTF-8"?>
<Server port="-1">
  <Service name="Catalina">
    <Connector address="127.0.0.1" port="8080" protocol="HTTP/1.1" connectionTimeout="20000" URIEncoding="UTF-8" />
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="false" />
    </Engine>
  </Service>
</Server>
'@ | Set-Content -LiteralPath $server -Encoding UTF8
}

$previousPassword = $env:DB_PASSWORD
$secret = $null
try {
    if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD) -and !$SkipPasswordPrompt) {
        $secret = Read-Host 'Senha de contratofacil_app (nao sera gravada)' -AsSecureString
        $env:DB_PASSWORD = (New-Object System.Net.NetworkCredential('', $secret)).Password
        if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) { throw 'Informe a senha da aplicacao.' }
    }
    if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) { Write-Warning 'Sem DB_PASSWORD: somente diagnostico das paginas; CRUD bloqueado.' }
    Copy-Item -LiteralPath $war -Destination (Join-Path $base 'webapps/contratofacil.war') -Force
    $jdkDirectory = $env:JAVA_HOME
    if (!$jdkDirectory) {
        # Evita o lancador javapath da Oracle, que cria um processo intermediario.
        $info = New-Object Diagnostics.ProcessStartInfo
        $info.FileName = (Get-Command java).Source
        $info.Arguments = '-XshowSettings:properties -version'
        $info.UseShellExecute = $false
        $info.CreateNoWindow = $true
        $info.RedirectStandardError = $true
        $probeJava = [Diagnostics.Process]::Start($info)
        try {
            $properties = $probeJava.StandardError.ReadToEnd()
            $probeJava.WaitForExit()
            $jdkDirectory = [regex]::Match($properties, '(?m)^\s*java.home\s*=\s*(.+)$').Groups[1].Value.Trim()
        } finally { $probeJava.Dispose() }
    }
    if (!$jdkDirectory) { throw 'Configure JAVA_HOME com o diretorio do JDK 21.' }
    $java = Join-Path $jdkDirectory 'bin/java.exe'
    Write-Host 'Tomcat: http://localhost:8080/contratofacil/ | Logs neste terminal. Use a tarefa parar Tomcat para encerrar.'
    $javaArgs = @(
        '-Dfile.encoding=UTF-8', "-Dcatalina.home=$TomcatHome", "-Dcatalina.base=$base",
        "-Djava.io.tmpdir=$base/temp", "-Djava.util.logging.config.file=$base/conf/logging.properties",
        '-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager',
        '-cp', "$TomcatHome/bin/bootstrap.jar;$TomcatHome/bin/tomcat-juli.jar",
        'org.apache.catalina.startup.Bootstrap', 'start'
    )
    & $java @javaArgs
    # A parada solicitada por -Stop tambem encerra esta chamada nativa.
} finally {
    $env:DB_PASSWORD = $previousPassword
    if ($secret) { $secret.Dispose() }
}
