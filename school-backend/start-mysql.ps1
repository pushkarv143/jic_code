# =====================================================================
# Starts the local MySQL if it is not already listening, then exits.
#
# Wired into IntelliJ as a "before launch" task of the
# SchoolManagementApplication run configuration, so clicking Run brings
# the database up first. Safe to run by hand too.
#
# THE LOCAL MYSQL IS NOT ON 3306.
#
# C:\ProgramData\MySQL\my.ini sets port=3333, and the install has no
# registered Windows service, so nothing listens until this script (or
# you) starts mysqld. That is why the backend's default DB_URL -
# jdbc:mysql://localhost:3306/... - fails with "Communications link
# failure" on a machine that does have MySQL installed and the schema
# loaded.
#
# If you would rather MySQL just always be up, register it as a Windows
# service once (elevated prompt) and delete the before-launch task:
#   & 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' --install MySQL84 --defaults-file='C:\ProgramData\MySQL\my.ini'
#   Start-Service MySQL84; Set-Service MySQL84 -StartupType Automatic
# =====================================================================

[CmdletBinding()]
param(
    [string] $MysqlHome = 'C:\Program Files\MySQL\MySQL Server 8.4',
    [string] $MysqlDefaultsFile = 'C:\ProgramData\MySQL\my.ini',
    [int]    $DbPort = 3333,
    [string] $DbName = 'school_management_system',
    [string] $DbUser = 'root'
)

$ErrorActionPreference = 'Stop'

$mysqld = Join-Path $MysqlHome 'bin\mysqld.exe'
$mysql  = Join-Path $MysqlHome 'bin\mysql.exe'

function Test-DbPort {
    return [bool] (Get-NetTCPConnection -LocalPort $DbPort -State Listen -ErrorAction SilentlyContinue)
}

if (Test-DbPort) {
    Write-Host "MySQL already listening on $DbPort."
}
else {
    if (-not (Test-Path $mysqld)) {
        Write-Host "mysqld.exe not found at $mysqld"
        Write-Host "Edit -MysqlHome in this script, or in the run configuration's Script options."
        exit 1
    }

    Write-Host "Starting MySQL on port $DbPort ..."
    # Detached, so it outlives this task and the IDE run that triggered it.
    # Stop it with: Get-Process mysqld | Stop-Process
    Start-Process -FilePath $mysqld `
                  -ArgumentList "--defaults-file=`"$MysqlDefaultsFile`"" `
                  -WindowStyle Hidden

    $deadline = (Get-Date).AddSeconds(60)
    while (-not (Test-DbPort) -and (Get-Date) -lt $deadline) {
        Start-Sleep -Milliseconds 1000
    }

    if (-not (Test-DbPort)) {
        Write-Host "MySQL did not come up within 60s."
        Write-Host "Check the error log in the datadir named after this machine, e.g."
        Write-Host "  C:\ProgramData\MySQL\MySQL Server 8.4\Data\$env:COMPUTERNAME.err"
        exit 1
    }
    Write-Host "MySQL is up."
}

# ---------------------------------------------------------------------
# Schema check - report only, never fail the launch.
#
# ddl-auto is `none`, so the application never creates or alters a table.
# A database missing a migration therefore fails at query time with an
# "Unknown column" deep in a stack trace rather than at startup. Checking
# the columns/tables the most recent migrations add turns that into a
# message naming the file to run.
# ---------------------------------------------------------------------
if (Test-Path $mysql) {
    $checkSql = @"
SELECT
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema='$DbName' AND table_name='students' AND column_name='first_name') AS student_identity,
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema='$DbName' AND table_name='study_materials') AS study_materials,
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema='$DbName' AND table_name='class_officials') AS class_module;
"@
    # mysql.exe writes an advisory warning to stderr on every invocation. Under
    # $ErrorActionPreference = 'Stop', Windows PowerShell turns any stderr line
    # from a native command into a terminating NativeCommandError - so left as a
    # plain call this check would abort the launch on a machine where nothing is
    # actually wrong. Relax the preference around the call and restore it after.
    $previousEap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $result = $checkSql | & $mysql -u $DbUser -h 127.0.0.1 -P $DbPort --skip-password --batch --skip-column-names $DbName 2>$null
    }
    catch {
        # An unreachable or unauthenticated database is not this check's
        # problem; the backend's own connection attempt reports it far
        # more clearly.
        $result = $null
    }
    finally {
        $ErrorActionPreference = $previousEap
    }

    if ($result) {
        $cols = ($result -split "`t")
        $pending = @()
        if ($cols[0] -eq '0') { $pending += '11_student_identity.sql' }
        if ($cols[1] -eq '0') { $pending += '09_study_materials.sql' }
        if ($cols[2] -eq '0') { $pending += '12_class_module.sql' }

        if ($pending.Count -gt 0) {
            Write-Host ""
            Write-Host "PENDING MIGRATIONS - the backend will fail on the affected queries:"
            $pending | Sort-Object | ForEach-Object { Write-Host "  database/$_" }
            Write-Host ""
            Write-Host "Apply them from the repo root, in filename order:"
            Write-Host "  & '$mysql' -u $DbUser -h 127.0.0.1 -P $DbPort --skip-password $DbName -e `"source database/<file>`""
            Write-Host ""
        }
        else {
            Write-Host "Schema looks current."
        }
    }
}

exit 0
