# Clean, Compile and Run Java Project

Write-Host "=== Cleaning old class files ===" -ForegroundColor Yellow
Get-ChildItem -Path . -Filter *.class -Recurse | Remove-Item -Force
Write-Host "Deleted all .class files" -ForegroundColor Green

Write-Host "`n=== Compiling Java files ===" -ForegroundColor Yellow
$javaFiles = Get-ChildItem -Path . -Filter *.java -Recurse | Select-Object -ExpandProperty FullName
javac $javaFiles

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful!" -ForegroundColor Green
    
    Write-Host "`n=== Running Main ===" -ForegroundColor Yellow
    java Main
} else {
    Write-Host "Compilation failed!" -ForegroundColor Red
    exit 1
}
