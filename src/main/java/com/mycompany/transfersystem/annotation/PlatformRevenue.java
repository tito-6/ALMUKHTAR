package com.mycompany.transfersystem.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as generating platform revenue. The revenue aspect will collect the fee
 * and credit PLATFORM_OWNER after the method completes successfully.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlatformRevenue {

    String event();

    String rateKey() default "";
}
