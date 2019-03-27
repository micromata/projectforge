package org.projectforge.test.poorman;

import java.util.Map;

import org.projectforge.test.AbstractTestNGBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.testng.annotations.Test;

public class SpringBeanTest extends AbstractTestNGBase
{

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SpringBeanTest.class);

  @Autowired
  AbstractApplicationContext abstractApplicationContext;

  @Autowired
  ApplicationContext applicationContext;

  @Test
  public void getAllBeansTest()
  {
    ConfigurableListableBeanFactory beanFactory = abstractApplicationContext.getBeanFactory();
    for (String name : abstractApplicationContext.getBeanDefinitionNames()) {
      Object bean = beanFactory.getBean(name);
      log.info("Bean class name: " + bean.getClass().getName());
    }
  }

  @Test
  public void getAnnotatedBeanTest()
  {
    Map<String, Object> beans = applicationContext.getBeansWithAnnotation(MyTestAnnotation.class);
    for (String s : beans.keySet()) {
      Object o = beans.get(s);
      if (o instanceof MyTestComponent) {
        MyTestComponent component = (MyTestComponent) o;
        component.sayHello();
      }
    }
  }

  @Test
  public void getBeansByTypeTest()
  {
    Map<String, MyTestInterface> beans = applicationContext.getBeansOfType(MyTestInterface.class);
    for (MyTestInterface component : beans.values()) {
      component.sayHello();
    }
  }

}
