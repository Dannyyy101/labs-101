package com.labs_101.backend.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Id of the signed in user, the {@code sub} claim of the access token.
 * Replaces the user id that used to be part of the path, so a user can only
 * reach their own data.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "subject")
public @interface CurrentUser {
}
