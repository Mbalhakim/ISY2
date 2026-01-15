Write-Host "=== Compiling ServerTest ===" -ForegroundColor Cyan

# Compile all necessary files
javac -encoding UTF-8 network/ServerClient.java network/ServerListener.java ServerTest.java

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "=== Running ServerTest ===" -ForegroundColor Cyan
    java ServerTest
} else {
    Write-Host "Compilation failed!" -ForegroundColor Red
    exit 1
}
