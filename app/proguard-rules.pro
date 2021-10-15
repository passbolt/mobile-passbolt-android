# do not obfuscate code - Open Source
-dontobfuscate

# keep all the code from the app
-keep class com.passbolt.mobile.android.** { *; }

# General config
-keepattributes InnerClasses,Signature,Exceptions,EnclosingMethod,SourceFile,LineNumberTable,*Annotation*
-renamesourcefileattribute SourceFile

# Room & SqlCipher
-keep,includedescriptorclasses class net.sqlcipher.** { *; }
-keep,includedescriptorclasses interface net.sqlcipher.** { *; }
