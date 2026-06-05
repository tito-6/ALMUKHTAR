package com.mycompany.transfersystem.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Money-moving endpoints must send a valid {@code Idempotency-Key} header (16–128 chars after trim).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireIdempotencyKey {
}
