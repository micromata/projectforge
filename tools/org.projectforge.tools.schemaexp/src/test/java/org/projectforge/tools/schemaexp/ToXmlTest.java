package org.projectforge.tools.schemaexp;

public class ToXmlTest
{
  //  @Test
  public void testExp()
  {
    System.getProperties().setProperty("projectForgeDs", "postgres");
    SchemaExpMain.main(new String[] { "-exp", "target/pf-dump.xml" });

  }
}
