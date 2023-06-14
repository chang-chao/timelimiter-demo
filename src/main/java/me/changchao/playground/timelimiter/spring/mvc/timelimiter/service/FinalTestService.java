package me.changchao.playground.timelimiter.spring.mvc.timelimiter.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FinalTestService {
  long value = 0;

  public long readValue() {
    return value;
  }
  ;
  // adding this annotations is just to force the object proxied by CGLIB
  @Transactional
  // any bean class method should never be declared as final
  public final void writeValue(long v) {
    value = v;
  }
}
