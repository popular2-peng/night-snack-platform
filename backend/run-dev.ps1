# ============================================================
# 夜宵外卖平台 — 开发环境启动脚本（Windows / PowerShell）
# 用法：
#   cd backend
#   .\run-dev.ps1
# 说明：
#   1. 若存在 backend\.env，自动加载其中的环境变量（.env 已 gitignore）
#   2. 启动 Spring Boot（dev profile，端口 8080），Ctrl+C 停止
# ============================================================

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# --- 1. 加载 .env（若存在） ---
if (Test-Path ".env") {
    Write-Host "[run-dev] 从 .env 加载环境变量..." -ForegroundColor Cyan
    Get-Content ".env" | ForEach-Object {
        $line = $_.Trim()
        # 跳过空行和注释行
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
    Write-Host "[run-dev] 未找到 .env（可复制 .env.example 为 .env 注入真实密钥）" -ForegroundColor Yellow
}

# --- 2. 启动 Spring Boot ---
mvn -pl .nsp-server spring-boot:run
