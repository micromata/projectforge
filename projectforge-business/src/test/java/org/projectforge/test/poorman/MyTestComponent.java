package org.projectforge.test.poorman;

import org.springframework.stereotype.Component;

@Component
@MyTestAnnotation
public class MyTestComponent implements MyTestInterface
{

  private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MyTestComponent.class);

  public void sayHello()
  {
    log.info("######## HELLO!!! ########");
  }

}
