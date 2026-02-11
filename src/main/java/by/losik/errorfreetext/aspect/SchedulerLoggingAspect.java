package by.losik.errorfreetext.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Aspect
@Component
public class SchedulerLoggingAspect {

    @Pointcut("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void scheduledMethods() {}

    @Pointcut("within(by.losik.errorfreetext.scheduler..*)")
    public void schedulerClasses() {}

    @Around("scheduledMethods() && schedulerClasses()")
    public Object logScheduledMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        Scheduled scheduledAnnotation = getScheduledAnnotation(joinPoint);
        String schedulerInfo = getSchedulerInfo(scheduledAnnotation);

        log.info("SCHEDULER_START - {}.{}() {}", className, methodName, schedulerInfo);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            long executionTime = stopWatch.getTotalTimeMillis();
            log.info("SCHEDULER_SUCCESS - {}.{}() completed in {} ms",
                    className, methodName, executionTime);

            return result;

        } catch (Exception e) {
            stopWatch.stop();

            log.error("SCHEDULER_ERROR - {}.{}() failed after {} ms: {} - {}",
                    className, methodName, stopWatch.getTotalTimeMillis(),
                    e.getClass().getSimpleName(), e.getMessage(), e);

            throw e;
        }
    }

    private Scheduled getScheduledAnnotation(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.getTarget()
                    .getClass()
                    .getMethod(joinPoint.getSignature().getName())
                    .getAnnotation(Scheduled.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private String getSchedulerInfo(Scheduled scheduled) {
        if (scheduled == null) {
            return "";
        }

        StringBuilder info = new StringBuilder("[");

        if (!scheduled.fixedDelayString().isEmpty()) {
            info.append("fixedDelay=").append(scheduled.fixedDelayString());
        } else if (scheduled.fixedDelay() > 0) {
            info.append("fixedDelay=").append(scheduled.fixedDelay()).append("ms");
        }

        if (!scheduled.fixedRateString().isEmpty()) {
            if (info.length() > 1) info.append(", ");
            info.append("fixedRate=").append(scheduled.fixedRateString());
        } else if (scheduled.fixedRate() > 0) {
            if (info.length() > 1) info.append(", ");
            info.append("fixedRate=").append(scheduled.fixedRate()).append("ms");
        }

        if (!scheduled.cron().isEmpty()) {
            if (info.length() > 1) info.append(", ");
            info.append("cron=").append(scheduled.cron());
        }

        info.append("]");

        return info.toString();
    }
}