# 液态玻璃 / backdrop 相关（来自 kyant-backdrop 源码模块）
-keep class com.kyant.backdrop.** { *; }
-dontwarn com.kyant.backdrop.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.wencui.quiz.**$$serializer { *; }
-keepclassmembers class com.wencui.quiz.** { *** Companion; }
-keepclasseswithmembers class com.wencui.quiz.** { kotlinx.serialization.KSerializer serializer(...); }