package org.projectforge.test.poorman;

import org.springframework.stereotype.Component;

@Component
@MyTestAnnotation
public class MyTestComponent implements MyTestInterface
{

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MyTestComponent.class);

  @Override
  public void sayHello()
  {
    log.info("######## HELLO!!! ########");
  }

}
