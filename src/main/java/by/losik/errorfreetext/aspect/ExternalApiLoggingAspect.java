package by.losik.errorfreetext.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Aspect
@Component
public class ExternalApiLoggingAspect {

    @Pointcut("within(@org.springframework.stereotype.Component *)")
    public void componentMethods() {}

    @Pointcut("execution(* by.losik.errorfreetext.external..*.*(..))")
    public void externalPackageMethods() {}

    @Around("componentMethods() && externalPackageMethods()")
    public Object logExternalApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        Object[] args = joinPoint.getArgs();
        String argsLog = Arrays.stream(args)
                .map(arg -> {
                    if (arg instanceof String str) {
                        return str.length() > 100 ? str.substring(0, 100) + "..." : str;
                    } else if (arg instanceof List<?> list) {
                        return "List[" + list.size() + " items]";
                    }
                    return String.valueOf(arg);
                })
                .collect(java.util.stream.Collectors.joining(", "));

        log.debug("EXTERNAL_API - ENTER: {}.{}({})", className, methodName, argsLog);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            log.debug("EXTERNAL_API - EXIT: {}.{}() [{} ms] - Success",
                    className, methodName, stopWatch.getTotalTimeMillis());

            return result;

        } catch (Exception e) {
            stopWatch.stop();

            log.error("EXTERNAL_API - ERROR: {}.{}() [{} ms] - {}: {}",
                    className, methodName, stopWatch.getTotalTimeMillis(),
                    e.getClass().getSimpleName(), e.getMessage());

            throw e;
        }
    }
}