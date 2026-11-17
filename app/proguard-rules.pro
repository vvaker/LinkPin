# LinkPin MVP：debug 侧载为主，release 不混淆。
# 如未来开启 minify，需为 kotlinx.serialization 保留以下规则：
# -keepattributes *Annotation*, InnerClasses
# -dontnote kotlinx.serialization.**
# -keepclassmembers class com.local.linkpin.data.** { *** Companion; }
