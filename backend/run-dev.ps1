# ============================================================
# Night Snack Platform - dev startup script (Windows / PowerShell)
# Usage:
#   cd backend
#   .\run-dev.ps1
# Notes:
#   1. Loads backend\.env if present (.env is gitignored, never commit it)
#   2. Starts Spring Boot (dev profile, port 8080). Ctrl+C to stop.
# ============================================================

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# --- 1. Load .env (if present) ---
if (Test-Path ".env") {
    Write-Host "[run-dev] Loading environment variables from .env ..." -ForegroundColor Cyan
    Get-Content ".env" | ForEach-Object {
        $line = $_.Trim()
        # skip blank lines and comment lines
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $name, $value = $line -split "=", 2
            $name  = $name.Trim()
            $value = $value.Trim().Trim('"').Trim("'")
            if ($name) {
                [Environment]::SetEnvironmentVariable($name, $value, "Process")
            }
        }
    }
} else {
    Write-Host "[run-dev] .env not found (copy .env.example to .env and fill real keys)" -ForegroundColor Yellow
}

# --- 2. Start Spring Boot ---
mvn -pl .nsp-server spring-boot:run
