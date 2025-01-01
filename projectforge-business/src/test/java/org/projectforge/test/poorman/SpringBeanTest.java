/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.test.poorman;

import org.junit.jupiter.api.Test;
import org.projectforge.business.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.Map;

public class SpringBeanTest extends AbstractTestBase
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
