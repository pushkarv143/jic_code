# =====================================================================
# Builds the backend and deploys it to the server.
#
# Run from the repository root:
#     .\deploy\deploy.ps1 -KeyFile C:\path\to\key.pem
#
# Requires an OpenSSH key (.pem). PuTTY .ppk will not work with ssh/scp
# — convert first with PuTTYgen: Load -> Conversions -> Export OpenSSH key.
# =====================================================================
param(
    [string]$ServerHost = '132.226.191.38',
    [string]$ServerUser = 'ubuntu',
    [Parameter(Mandatory = $true)][string]$KeyFile,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$RemoteApp = '/opt/school-backend'
$Jar = 'school-backend/target/school-backend.jar'

if (-not (Test-Path $KeyFile)) { throw "Key file not found: $KeyFile" }

if (-not $SkipBuild) {
    Write-Host '==> Building jar' -ForegroundColor Cyan
    Push-Location school-backend
    try {
        & .\mvnw.cmd -q clean package -DskipTests
        if ($LASTEXITCODE -ne 0) { throw 'Maven build failed' }
    } finally { Pop-Location }
}

if (-not (Test-Path $Jar)) { throw "Jar not found at $Jar - run without -SkipBuild" }
$size = [math]::Round((Get-Item $Jar).Length / 1MB, 1)
Write-Host "==> Jar ready ($size MB)" -ForegroundColor Green

$ssh = @('-i', $KeyFile, '-o', 'StrictHostKeyChecking=accept-new')
$target = "$ServerUser@$ServerHost"

# Upload to a staging name first, then move into place after stopping the
# service. Copying straight over a running jar risks a partial file being
# left behind if the transfer drops.
Write-Host '==> Uploading' -ForegroundColor Cyan
& scp @ssh $Jar "${target}:/tmp/school-backend.jar.new"
if ($LASTEXITCODE -ne 0) { throw 'scp failed' }

Write-Host '==> Swapping in and restarting' -ForegroundColor Cyan
$remote = @"
set -e
sudo systemctl stop school-backend || true
# Keep one rollback copy - a bad deploy is far easier to undo than to rebuild.
[ -f $RemoteApp/school-backend.jar ] && sudo cp $RemoteApp/school-backend.jar $RemoteApp/school-backend.jar.prev
sudo mv /tmp/school-backend.jar.new $RemoteApp/school-backend.jar
sudo chown ubuntu:ubuntu $RemoteApp/school-backend.jar
sudo systemctl start school-backend
sleep 8
sudo systemctl is-active school-backend
"@
& ssh @ssh $target $remote
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Service did not come up. Check the logs:' -ForegroundColor Red
    Write-Host "  ssh -i $KeyFile $target 'journalctl -u school-backend -n 60 --no-pager'" -ForegroundColor Yellow
    exit 1
}

Write-Host '==> Health check' -ForegroundColor Cyan
& ssh @ssh $target 'curl -sf http://localhost:8080/actuator/health || echo "health endpoint not responding yet"'

Write-Host ''
Write-Host 'Deployed.' -ForegroundColor Green
Write-Host "Logs: ssh -i $KeyFile $target 'journalctl -u school-backend -f'"
