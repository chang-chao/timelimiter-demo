package me.changchao.playground.timelimiter.spring.mvc.timelimiter.controller;

import me.changchao.playground.timelimiter.spring.mvc.timelimiter.service.FinalTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/final")
public class FinalTestController {
  @Autowired FinalTestService finalTestService;

  @GetMapping("/read")
  public long read() {
    return finalTestService.readValue();
  }

  @PostMapping("/write/{value}")
  public long write(@PathVariable("value") long value) {
    finalTestService.writeValue(value);
    return finalTestService.readValue();
  }
}
