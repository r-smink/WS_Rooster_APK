# OkHttp3
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Moshi
-keep class com.roosterplanner.app.data.** { *; }
-keep @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
