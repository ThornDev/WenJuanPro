# Hilt / Dagger generated code
-keep,allowobfuscation,allowshrinking class dagger.hilt.** { *; }
-keep,allowobfuscation,allowshrinking class * extends dagger.hilt.android.internal.managers.* { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class ai.wenjuanpro.app.**$$serializer { *; }
-keepclassmembers class ai.wenjuanpro.app.** {
    *** Companion;
}
-keepclasseswithmembers class ai.wenjuanpro.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Timber
-dontwarn org.jetbrains.annotations.**
