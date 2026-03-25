#!/bin/bash
sed -i 's/AppDatabase.MIGRATION_13_14)/AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15)/g' app/src/main/java/com/pulse/di/AppModule.kt
