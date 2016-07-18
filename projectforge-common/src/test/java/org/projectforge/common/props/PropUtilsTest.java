package org.projectforge.common.props;

import org.projectforge.common.anots.PropertyInfo;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PropUtilsTest
{

  private final String i18nKey = "prop.test";

  @Test
  public void testGet() throws Exception
  {
    Object o = new Object();
    Assert.assertNull(PropUtils.get(o.getClass(), "noClass"));
    Integer integer = new Integer(42);
    Assert.assertNull(PropUtils.get(integer.getClass().getDeclaredField("value")));

    TestProp testProp = new TestProp();
    Assert.assertNotNull(PropUtils.get(TestProp.class, "property"));

    Assert.assertNull(PropUtils.get(null));

  }

  @Test
  public void getField()
  {
    Integer integer = new Integer(12);

    Assert.assertNotNull(PropUtils.getField(Integer.class, "value"));
    Assert.assertNull(PropUtils.getField(Object.class, "value"));
    Assert.assertNotNull(PropUtils.getField(TestProp.class, "property"));
  }

  @Test
  public void testGetI18NKey()
  {
    Assert.assertNull(PropUtils.getI18nKey(Integer.class, "class"));
    Assert.assertEquals(PropUtils.getI18nKey(TestProp.class, "property"), i18nKey);
  }

  @Test
  public void testGetPropertyInfoFields()
  {
    Assert.assertEquals(PropUtils.getPropertyInfoFields(TestProp.class).length, 1);
    Assert.assertEquals(PropUtils.getPropertyInfoFields(TestProp.class).length, 1);
    Assert.assertEquals(PropUtils.getPropertyInfoFields(Object.class).length, 0);
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
