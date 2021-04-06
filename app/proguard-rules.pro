# Add Application specific rules here.

# General config
-keepattributes InnerClasses,Signature,Exceptions,EnclosingMethod,SourceFile,LineNumberTable,*Annotation*
-renamesourcefileattribute SourceFile

-keepclassmembers class * extends java.lang.Enum {
    <fields>;
}

# Exceptions
-keepclasseswithmembernames class * extends java.lang.Throwable

# AndroidX Navigation
-keepnames class androidx.navigation.fragment.NavHostFragment
