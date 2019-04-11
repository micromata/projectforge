package org.projectforge.common.props;

import org.projectforge.common.anots.PropertyInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PropUtilsTest
{

  private final String i18nKey = "prop.test";

  @Test
  public void testGet() throws Exception
  {
    Object o = new Object();
    Assertions.assertNull(PropUtils.get(o.getClass(), "noClass"));
    Integer integer = new Integer(42);
    Assertions.assertNull(PropUtils.get(integer.getClass().getDeclaredField("value")));

    TestProp testProp = new TestProp();
    Assertions.assertNotNull(PropUtils.get(TestProp.class, "property"));

    Assertions.assertNull(PropUtils.get(null));

  }

  @Test
  public void getField()
  {
    Integer integer = new Integer(12);

    Assertions.assertNotNull(PropUtils.getField(Integer.class, "value"));
    Assertions.assertNull(PropUtils.getField(Object.class, "value"));
    Assertions.assertNotNull(PropUtils.getField(TestProp.class, "property"));
  }

  @Test
  public void testGetI18NKey()
  {
    Assertions.assertNull(PropUtils.getI18nKey(Integer.class, "class"));
    Assertions.assertEquals(PropUtils.getI18nKey(TestProp.class, "property"), i18nKey);
  }

  @Test
  public void testGetPropertyInfoFields()
  {
    Assertions.assertEquals(PropUtils.getPropertyInfoFields(TestProp.class).length, 1);
    Assertions.assertEquals(PropUtils.getPropertyInfoFields(TestProp.class).length, 1);
    Assertions.assertEquals(PropUtils.getPropertyInfoFields(Object.class).length, 0);
  }

  class TestProp
  {

    @PropertyInfo(i18nKey = i18nKey)
    private String property;

    public String getProperty()
    {
      return property;
    }

    public void setProperty(String property)
    {
      this.property = property;
    }
  }
}
