$ErrorActionPreference = "Stop"
$Root = Resolve-Path "$PSScriptRoot\.."
Remove-Item -Recurse -Force "$Root\backend\target" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "$Root\backend\data" -ErrorAction SilentlyContinue
Write-Host "État local backend supprimé : backend/target et backend/data"
Write-Host "À faire côté navigateur : vider les caches/service workers du site http://localhost:8080."
