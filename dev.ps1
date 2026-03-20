param (
    [switch]$Watch,
    [switch]$HotRefresh
)

if ($HotRefresh -or $Watch) {
    Write-Host "Pulse HotRefresh/Watch Mode Active: Monitoring for changes..." -ForegroundColor Green
    ./gradlew installDebug -t
} else {
    Write-Host "Building and Launching Pulse..." -ForegroundColor Cyan
    ./gradlew installDebug
}


