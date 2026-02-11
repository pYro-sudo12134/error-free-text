package by.losik.errorfreetext.aspect;

import by.losik.errorfreetext.entity.CorrectionTask;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class TaskConcurrencyLimiterAspect {

    private final Semaphore semaphore;

    public TaskConcurrencyLimiterAspect(
            @Value("${app.scheduler.max-concurrent-tasks:5}") int maxConcurrentTasks) {
        this.semaphore = new Semaphore(maxConcurrentTasks);
    }

    @Pointcut("execution(* by.losik.errorfreetext.scheduler.CorrectionTaskScheduler.processSingleTask(..))")
    public void processSingleTaskMethod() {}

    @Around("processSingleTaskMethod()")
    public Object limitConcurrency(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        if (args.length > 0 && args[0] instanceof CorrectionTask task) {
            log.debug("CONCURRENCY_LIMITER - Attempting to acquire semaphore for task: {}", task.getId());
            if (semaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                try {
                    log.debug("CONCURRENCY_LIMITER - Semaphore acquired for task: {}", task.getId());
                    return joinPoint.proceed();
                } finally {
                    semaphore.release();
                    log.debug("CONCURRENCY_LIMITER - Semaphore released for task: {}", task.getId());
                }
            } else {
                log.warn("CONCURRENCY_LIMITER - Timeout waiting for semaphore, skipping task: {}", task.getId());
                return null;
            }
        }

        return joinPoint.proceed();
    }
}