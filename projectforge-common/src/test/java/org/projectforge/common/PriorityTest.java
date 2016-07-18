package org.projectforge.common;

import org.projectforge.common.i18n.Priority;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.projectforge.common.i18n.Priority.*;

public class PriorityTest
{
  @Test
  public void testKeyAndI18NKey()
  {

    String highest = "highest";
    Assert.assertEquals(HIGHEST.getKey(), highest);
    Assert.assertEquals(HIGHEST.getI18nKey(), "priority." + highest);

    String high = "high";
    Assert.assertEquals(HIGH.getKey(), high);
    Assert.assertEquals(HIGH.getI18nKey(), "priority." + high);

    String middle = "middle";
    Assert.assertEquals(MIDDLE.getKey(), middle);
    Assert.assertEquals(MIDDLE.getI18nKey(), "priority." + middle);

    String low = "low";
    Assert.assertEquals(LOW.getKey(), low);
    Assert.assertEquals(LOW.getI18nKey(), "priority." + low);

    String least = "least";
    Assert.assertEquals(LEAST.getKey(), least);
    Assert.assertEquals(LEAST.getI18nKey(), "priority." + least);
  }

  @Test
  public void testGetPriority()
  {
    Assert.assertNull(Priority.getPriority(""));
    Assert.assertEquals(Priority.getPriority("LEAST"), LEAST);
    Assert.assertEquals(Priority.getPriority("LOW"), LOW);
    Assert.assertEquals(Priority.getPriority("MIDDLE"), MIDDLE);
    Assert.assertEquals(Priority.getPriority("HIGH"), HIGH);
    Assert.assertEquals(Priority.getPriority("HIGHEST"), HIGHEST);
    try {
      Priority.getPriority("Extrordinary High");
    } catch (UnsupportedOperationException e) {
      Assert.assertNotNull(e);
    }
  }

}
