package org.projectforge.tools.schemaexp;

import org.junit.jupiter.api.Test;

public class ClearDbTest
{
  @Test
  public void testImp()
  {
    //    System.getProperties().setProperty("projectForgeDs", "postgres2");
    SchemaExpMain.main(new String[] { "-cleardb", });
  }
}
