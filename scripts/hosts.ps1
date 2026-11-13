$hosts = Join-Path $env:SystemRoot 'System32\drivers\etc\hosts'
$entry = '127.0.0.1 prueba-accenture.com'

if (-not (Select-String -Path $hosts -Pattern 'prueba-accenture\.com' -Quiet)) {
    Add-Content -Path $hosts -Value $entry
    Write-Host "Entrada agregada: $entry"
}
else {
    Write-Host "La entrada ya existe en $hosts"
}

ipconfig /flushdns | Out-Null
Select-String -Path $hosts -Pattern 'prueba-accenture'
