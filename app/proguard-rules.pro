# TDLib JNI rules
-keep class org.drinkless.tdlib.TdApi { *; }
-keep class org.drinkless.tdlib.TdApi$* { *; }
-keep class org.drinkless.tdlib.Client { *; }
-keep class org.drinkless.tdlib.Client$* { *; }
-keep class org.drinkless.tdlib.Log { *; }

# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Room
-keep class androidx.room.concurrent.TableWatcher { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# TDLib JNI preservation
-keep class org.drinkless.tdlib.** { *; }

# Room Database preservation
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
