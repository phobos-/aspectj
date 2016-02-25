package com.example;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/*
Based on:
https://github.com/jcabi/jcabi-aspects/blob/jcabi-0.15.2/src/main/java/com/jcabi/aspects/aj/MethodLogger.java
 */
@Aspect
public class PerformanceLogger {

    private static final double MEGABYTE = 1024.0f * 1024.0f;

    private double getMemoryConsumptionMb(){
        System.gc();
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / MEGABYTE;
    }

    @SuppressWarnings("SpringAopErrorsInspection")
    @Around("(execution(* *(..)) || initialization(*.new(..)))"
                    + " && @annotation(Loggable)")
    public Object wrapMethod(final ProceedingJoinPoint point) throws Throwable {
        final Method method =
                MethodSignature.class.cast(point.getSignature()).getMethod();
        return this.wrap(point, method, method.getAnnotation(Loggable.class));
    }

    @SuppressWarnings("SpringAopErrorsInspection")
    @Around("(execution(public * (@Loggable *).*(..))"
                    + " || initialization((@Loggable *).new(..)))"
                    + " && !execution(String *.toString())"
                    + " && !execution(int *.hashCode())"
                    + " && !execution(boolean *.canEqual(Object))"
                    + " && !execution(boolean *.equals(Object))"
                    + " && !cflow(call(com.example.PerformanceLogger.new()))")
    public Object wrapClass(final ProceedingJoinPoint point) throws Throwable {
        final Method method =
                MethodSignature.class.cast(point.getSignature()).getMethod();
        Object output;
        if (method.isAnnotationPresent(Loggable.class)) {
            output = point.proceed();
        } else {
            output = this.wrap(
                    point,
                    method,
                    method.getDeclaringClass().getAnnotation(Loggable.class)
            );
        }
        return output;
    }

    private Object wrap(final ProceedingJoinPoint point, final Method method, final Loggable annotation) throws Throwable {
        try {
            if (annotation.enabled()) {
                long start = System.currentTimeMillis();
                double before = getMemoryConsumptionMb();
                Object result = point.proceed();
                System.out.println( //TODO: log instead
                        "#" + method.getName() + String.format("() Time: %s[msec] Memory diff: %.2f[Mb]", System.currentTimeMillis() - start, getMemoryConsumptionMb() - before)
                );
                return result;
            } else return point.proceed();
        } catch (Throwable e){
            //TODO: log
            throw e;
        }
    }
}
