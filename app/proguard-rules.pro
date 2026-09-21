# ProGuard / R8 rules for BUDDYS (com.aura.glasschat)

# Preserve annotations and generics
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep all Firestore / Application data models and their getters/setters/properties
-keep class com.aura.glasschat.data.model.** { *; }
-keepclassmembers class com.aura.glasschat.data.model.** {
    <fields>;
    <init>(...);
    public *;
}

# Keep Update Engine models and classes
-keep class com.aura.glasschat.data.update.** { *; }
-keepclassmembers class com.aura.glasschat.data.update.** {
    <fields>;
    <init>(...);
    public *;
}

# Keep Hardware-Backed Encrypted Session Vault and Cryptography
-keep class com.aura.glasschat.data.security.** { *; }
-keepclassmembers class com.aura.glasschat.data.security.** {
    <fields>;
    <init>(...);
    public *;
}
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# Keep WebRTC JNI and classes
-keep class org.webrtc.** { *; }
-keepclassmembers class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Keep Firebase and Google Play Services (Auth, Nearby, Code Scanner)
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { *; }

# OkHttp & Okio
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**

# ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**


