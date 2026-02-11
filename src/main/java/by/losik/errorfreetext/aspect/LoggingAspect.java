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

@Slf4j
@Aspect
@Component
public class LoggingAspect {
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceMethods() {}
    @Pointcut("within(@org.springframework.stereotype.Repository *)")
    public void repositoryMethods() {}
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerMethods() {}

    @Around("serviceMethods()")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "SERVICE");
    }

    @Around("repositoryMethods()")
    public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "REPOSITORY");
    }

    @Around("controllerMethods()")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        log.debug("[CONTROLLER] {}.{} - START", className, methodName);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            log.debug("[CONTROLLER] {}.{} - COMPLETED [{} ms]",
                    className, methodName, stopWatch.getTotalTimeMillis());

            return result;

        } catch (Exception e) {
            stopWatch.stop();
            log.error("[CONTROLLER] {}.{} - FAILED [{} ms]: {}",
                    className, methodName, stopWatch.getTotalTimeMillis(),
                    e.getMessage(), e);
            throw e;
        }
    }

    private Object logMethodExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

        String className = methodSignature.getDeclaringType().getSimpleName();
        String methodName = methodSignature.getName();
        Object[] args = joinPoint.getArgs();

        if (log.isDebugEnabled()) {
            log.debug("[{}] {}.{}({}) - ENTER",
                    layer, className, methodName,
                    args.length > 0 ? Arrays.toString(args) : "");
        } else if (log.isInfoEnabled() && "CONTROLLER".equals(layer)) {
            log.info("[{}] {}.{}({})",
                    layer, className, methodName,
                    args.length > 0 ? Arrays.toString(args) : "");
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            if (log.isDebugEnabled()) {
                log.debug("[{}] {}.{}() - EXIT [{} ms] - Result: {}",
                        layer, className, methodName,
                        stopWatch.getTotalTimeMillis(),
                        result != null ? result.toString() : "null");
            }

            return result;

        } catch (Exception e) {
            stopWatch.stop();
            log.error("[{}] {}.{}() - ERROR [{} ms] - Exception: {}",
                    layer, className, methodName,
                    stopWatch.getTotalTimeMillis(),
                    e.getMessage(), e);

            throw e;
        }
    }
}