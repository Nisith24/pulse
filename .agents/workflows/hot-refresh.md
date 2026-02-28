---
description: Build, install and launch the Android app
---

// turbo-all
1. Build and install the app
```powershell
.\gradlew.bat installDebug
```

2. Start the main activity
```powershell
C:\Android\platform-tools\adb.exe shell am start -n com.pulse/.MainActivity
```