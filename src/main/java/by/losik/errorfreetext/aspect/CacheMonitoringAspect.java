package by.losik.errorfreetext.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class CacheMonitoringAspect {

    @Pointcut("@annotation(org.springframework.cache.annotation.Cacheable)")
    public void cacheableMethods() {}

    @Pointcut("@annotation(org.springframework.cache.annotation.CachePut)")
    public void cachePutMethods() {}

    @Pointcut("@annotation(org.springframework.cache.annotation.CacheEvict)")
    public void cacheEvictMethods() {}

    @AfterReturning(pointcut = "cacheableMethods()", returning = "result")
    public void logCacheableHit(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        if (result == null) {
            log.debug("CACHE_MISS - {}({}): Cache miss, fetched from source",
                    methodName, Arrays.toString(args));
        } else {
            log.debug("CACHE_HIT - {}({}): Retrieved from cache",
                    methodName, Arrays.toString(args));
        }
    }

    @AfterReturning(pointcut = "cachePutMethods()", returning = "result")
    public void logCachePut(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        log.debug("CACHE_PUT - {}: Updated cache with new value - {}", methodName, result);
    }

    @AfterReturning(pointcut = "cacheEvictMethods()")
    public void logCacheEvict(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        log.debug("CACHE_EVICT - {}: Removed from cache", methodName);
    }
}