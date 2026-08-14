# ---- kotlinx.serialization -------------------------------------------------
# The plugin generates a synthetic Companion.serializer() on every @Serializable
# class; R8 must not strip or rename it.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.greenwood.school.**$$serializer { *; }
-keepclassmembers class com.greenwood.school.** { *** Companion; }
-keepclasseswithmembers class com.greenwood.school.** { kotlinx.serialization.KSerializer serializer(...); }

# ---- Retrofit / OkHttp -----------------------------------------------------
-keepattributes Signature, Exceptions, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---- Hilt / Dagger ---------------------------------------------------------
-dontwarn dagger.hilt.**

# ---- Coroutines ------------------------------------------------------------
-dontwarn kotlinx.coroutines.**

# ---- Keep our model layer intact ------------------------------------------
# API DTO property names ARE the wire contract; obfuscating them breaks JSON.
-keep class com.greenwood.school.data.remote.dto.** { *; }
