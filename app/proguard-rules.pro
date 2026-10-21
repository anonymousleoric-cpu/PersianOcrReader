# Tesseract4Android (JNI bridge) must not be obfuscated
-keep class com.googlecode.tesseract.android.** { *; }
-dontwarn com.googlecode.tesseract.android.**
