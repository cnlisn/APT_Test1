package com.lisn.annotationlib;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

@Retention(CLASS)
@Target(FIELD)
public @interface AutoBundle {
    boolean require() default false;
}