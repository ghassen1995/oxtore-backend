package com.oxtore.backend.common.security;

import com.oxtore.backend.staff.enums.StaffRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireStoreRole {
    StaffRole[] value();
    String storeIdSource() default "path";
    String pathParam() default "";
}