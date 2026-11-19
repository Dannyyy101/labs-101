# retrofit, okhttp and kotlinx.serialization bring their own rules, these cover the generated client

# the interfaces of the generated client are only created through retrofit.create
-keep interface tech.project101.labs.api.apis.** { *; }

# the dtos are only touched through their generated serializers
-keep,includedescriptorclasses class tech.project101.labs.api.models.**$$serializer { *; }
-keepclassmembers class tech.project101.labs.api.models.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# the private response classes of the login
-keep,includedescriptorclasses class tech.project101.labs.auth.**$$serializer { *; }
