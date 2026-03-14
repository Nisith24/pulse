param (
    [switch]$Watch
)

if ($Watch) {
    Write-Host "Starting Pulse Dev Watcher... (App will re-install on save)" -ForegroundColor Cyan
    ./gradlew installDebug -t
} else {
    Write-Host "Building and Launching Pulse..." -ForegroundColor Green
    ./gradlew installDebug
    if ($?) {
        adb shell am start -n com.pulse/.MainActivity
    }
}
