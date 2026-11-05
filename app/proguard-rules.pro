-keepattributes *Annotation*, InnerClasses
-keepclassmembers class ru.kvizcap.quiz.app.** {
    *** Companion;
}
-keepclasseswithmembers class ru.kvizcap.quiz.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
