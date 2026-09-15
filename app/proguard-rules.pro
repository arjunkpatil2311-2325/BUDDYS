# ProGuard rules for GlassChat / BUDDYS

# Preserve annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep all Firestore / Application data models and their getters/setters/properties
-keep class com.aura.glasschat.data.model.** { *; }
-keepclassmembers class com.aura.glasschat.data.model.** {
    <fields>;
    <init>(...);
    public *;
}

# Keep WebRTC JNI and classes
-keep class org.webrtc.** { *; }
-keepclassmembers class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Keep Firebase and Google Play Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { *; }

# Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**

# ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

