package com.legalai.admin.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int qps() default 100;
    String key() default "default";
    int blockDuration() default 60;
}
