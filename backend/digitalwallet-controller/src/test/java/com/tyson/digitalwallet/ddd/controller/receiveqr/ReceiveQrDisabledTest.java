package com.tyson.digitalwallet.ddd.controller.receiveqr;
import static org.junit.Assert.*;
import com.tyson.digitalwallet.ddd.controller.resource.ReceiveQrController;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import java.util.Map;
public class ReceiveQrDisabledTest {
  @Test public void disabledControllerRequiresNoService() {
    try(var context=new AnnotationConfigApplicationContext()) {
      context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",Map.of("media.enabled","false")));
      context.register(ReceiveQrController.class);context.refresh();
      assertTrue(context.getBeansOfType(ReceiveQrController.class).isEmpty());
    }
  }
}
