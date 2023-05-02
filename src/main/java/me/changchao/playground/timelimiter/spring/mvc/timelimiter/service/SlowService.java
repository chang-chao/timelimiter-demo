package me.changchao.playground.timelimiter.spring.mvc.timelimiter.service;

import me.changchao.playground.timelimiter.spring.mvc.timelimiter.controller.FooController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SlowService {
  private static final Logger logger = LoggerFactory.getLogger(SlowService.class);

  public String twoSec() {
    try {
      // this won't be interrupted even in case of timeout
      // it is a limitation of of the annotation based method
      // https://github.com/resilience4j/resilience4j/issues/905
      // https://github.com/resilience4j/resilience4j/issues/905#issuecomment-595926206
      // Futures are not yet supported via annotations/aspects,
      // but only via the functional style.
      Thread.sleep(2000);
      logger.info("returning normal response");
      return "normal response";
    } catch (InterruptedException e) {
      logger.error("execution interrupted", e);
      return "interrupted response";
    }
  }

  public void compensate(String id) {
    logger.info("compensating {}", id);
  }
}
