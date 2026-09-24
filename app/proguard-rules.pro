# BlueType Proguard Rules

# Preserve Bluetooth HID callback methods and profiles accessed via system binder / reflection
-keep class android.bluetooth.BluetoothHidDevice** { *; }
-keep class * extends android.bluetooth.BluetoothHidDevice$Callback { *; }
-keep class * extends android.bluetooth.BluetoothProfile$ServiceListener { *; }

# Preserve HID data classes and reports
-keep class com.example.bluetype.hid.KeyReport { *; }
-keep class com.example.bluetype.hid.ConnectionState** { *; }

# Preserve DataStore preferences models
-keepclassmembers class com.example.bluetype.data.settings.AppSettings { *; }

# Keep Quick Settings Tile service
-keep class com.example.bluetype.service.SendClipboardTileService { *; }
-keep class com.example.bluetype.service.HidForegroundService { *; }
