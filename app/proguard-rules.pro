# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Jetpack Compose rules
-keepclassmembers class * {
    @androidx.compose.runtime.Composable class *;
    @androidx.compose.runtime.Composable void *(...);
}

# Room Database rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * implements androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-dontwarn androidx.room.paging.**

# Keep models and entities safe from obfuscation/obliteration
-keep class com.example.data.** { *; }
-keep class com.example.api.** { *; }

# Serialization / Moshi / Retrofit rules (generic protection)
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep the line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

