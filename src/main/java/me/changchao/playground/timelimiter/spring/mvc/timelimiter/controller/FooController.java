package me.changchao.playground.timelimiter.spring.mvc.timelimiter.controller;

import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.spring6.fallback.FallbackDecorators;
import io.github.resilience4j.spring6.fallback.FallbackMethod;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.annotation.PostConstruct;
import me.changchao.playground.timelimiter.spring.mvc.timelimiter.service.SlowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@RestController
@RequestMapping(value = "/foo")
public class FooController {
  private static final Logger logger = LoggerFactory.getLogger(FooController.class);
  public static final String TIME_LIMITER_NAME = "foo";
  @Autowired private TimeLimiterRegistry timeLimiterRegistry;

  @Autowired SlowService slowService;

  @Autowired FallbackDecorators fallbackDecorators;

  private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
  private Method foo2;

  // cancel does not work for annotation based method
  @Deprecated
  @GetMapping("/annotation")
  @TimeLimiter(name = TIME_LIMITER_NAME, fallbackMethod = "fallbackFoo")
  public CompletableFuture<String> foo(@RequestParam String id) {
    logger.info("foo started");
    return CompletableFuture.supplyAsync(slowService::twoSec);
  }

  private CompletableFuture<String> fallbackFoo(String id, TimeoutException timeoutException)
      throws TimeoutException {
    slowService.compensate(id);
    throw timeoutException;
  }

  @GetMapping("/functional")
  public

  // not work since CompletableFuture can't be  interrupted
  // CompletableFuture<String>

  Callable<String> foo2(@RequestParam String id) {
    logger.info("foo started");
    io.github.resilience4j.timelimiter.TimeLimiter timeLimiter =
        timeLimiterRegistry.timeLimiter(TIME_LIMITER_NAME);

    Supplier<CompletionStage<String>> origCompletionStageSupplier =
        () -> CompletableFuture.supplyAsync(slowService::twoSec);

    // still, CompletableFuture can't be  interrupted
    // This flag only works when you use TimeLimiter to decorate a method which returns a Future.
    // But unfortunately Futures are not yet supported via annotations/aspects, but only via the
    // functional style.
    // https://github.com/resilience4j/resilience4j/issues/1041

    // fallback is not supported in functional method!!!
    Callable<String> primaryCallable =
        timeLimiter
            // .executeCompletionStage(scheduler, origCompletionStageSupplier)
            .decorateFutureSupplier(() -> scheduler.submit(slowService::twoSec));
    try {
      FallbackMethod fallbackMethod =
          FallbackMethod.create("fallbackFoo2", foo2, new Object[] {id}, this);
      CheckedSupplier<Object> fallbackDecoratedSupplier =
          fallbackDecorators.decorate(fallbackMethod, primaryCallable::call);
      return convertSupplierToCallable(fallbackDecoratedSupplier, String.class);
    } catch (NoSuchMethodException e) {
      logger.warn("fallback method not found for foo2", e);
    }

    return primaryCallable;
  }

  private Callable<String> fallbackFoo2(String id, TimeoutException timeoutException)
      throws TimeoutException {
    slowService.compensate(id);
    throw timeoutException;
  }

  @ExceptionHandler({TimeoutException.class})
  public ResponseEntity<String> handleException() {
    return new ResponseEntity<>("internal timeout", HttpStatus.resolve(500));
  }

  @PostConstruct
  void postConstruct() throws Exception {
    io.github.resilience4j.timelimiter.TimeLimiter timeLimiter =
        timeLimiterRegistry.timeLimiter(TIME_LIMITER_NAME);
    io.github.resilience4j.timelimiter.TimeLimiter.EventPublisher eventPublisher =
        timeLimiter.getEventPublisher();
    eventPublisher.onTimeout(
        timeoutEvent -> {
          // we can't do the compensation
          // since there is no context in the event object
          logger.info("received timeout event from eventPublisher", timeoutEvent);
        });
    this.foo2 = this.getClass().getMethod("foo2", String.class);
  }

  private static <T> Callable<T> convertSupplierToCallable(
      CheckedSupplier<Object> fallbackDecoratedSupplier, Class<T> t) {
    return () -> {
      try {
        return (T) fallbackDecoratedSupplier.get();
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    };
  }
}
