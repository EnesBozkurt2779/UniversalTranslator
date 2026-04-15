-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

-keepattributes Signature
-keepattributes *Annotation*

-keep class com.translator.universal.data.model.** { *; }
-keep class com.translator.universal.data.service.** { *; }