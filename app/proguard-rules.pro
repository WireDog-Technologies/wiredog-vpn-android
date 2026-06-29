# Retain line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Moshi — keep all model classes used for JSON serialization
-keepclassmembers class com.wiredog.vpn.data.remote.api.dto.** { *; }
-keepclassmembers class com.wiredog.vpn.domain.model.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }

# Retrofit — keep service interfaces and their annotations
-keep interface com.wiredog.vpn.data.remote.api.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Hilt / Dagger — generated components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# WireGuard tunnel library
-keep class com.wireguard.** { *; }

# Enum classes (used in domain model)
-keepclassmembers enum * { *; }

# Google Crypto Tink — keep all classes and methods
-keep class com.google.crypto.tink.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.protobuf.**

# Error Prone annotations
-keep class com.google.errorprone.annotations.** { *; }
-dontwarn com.google.errorprone.annotations.**

# Security Crypto library
-keep class androidx.security.crypto.** { *; }
