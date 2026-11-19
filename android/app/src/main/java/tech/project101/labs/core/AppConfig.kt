package tech.project101.labs.core

import tech.project101.labs.BuildConfig

/** Settings from secrets.properties, which end up in BuildConfig at build time. */
object AppConfig {
    /** the backend including the /api prefix */
    val apiBaseUrl: String = BuildConfig.API_BASE_URL

    /** Zitadel, e.g. https://auth.project101.tech */
    val authIssuer: String = BuildConfig.AUTH_ISSUER.trimEnd('/')

    /** client id of the native app in Zitadel */
    val authClientId: String = BuildConfig.AUTH_CLIENT_ID

    /** without issuer and client id nobody can sign in */
    val isComplete: Boolean
        get() = authIssuer.isNotBlank() && authClientId.isNotBlank()

    /** the paths of the spec start with /api, so retrofit gets the server without it */
    val serverUrl: String
        get() = apiBaseUrl.trimEnd('/').removeSuffix("/api") + "/"

    /** the own profile in the Zitadel console, where name and picture are changed */
    val profileEditUrl: String
        get() = "$authIssuer/ui/console/users/me"
}
