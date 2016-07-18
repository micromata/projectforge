package org.projectforge.tools.schemaexp;

import org.testng.annotations.Test;

public class ClearDbTest
{
  @Test
  public void testImp()
  {
    //    System.getProperties().setProperty("projectForgeDs", "postgres2");
    SchemaExpMain.main(new String[] { "-cleardb", });
  }
}
