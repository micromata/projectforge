package org.projectforge.tools.schemaexp;

public class FromXmlTest
{
  //  @Test
  public void testImp()
  {
    System.getProperties().setProperty("projectForgeDs", "postgres2");
    //SchemaExpMain.main(new String[] { "-imp", "-cleardb", "-insertall", "target/pf-dump.xml" });
    SchemaExpMain.main(new String[] { "-imp", "-cleardb", "-insertall",
        "../../projectforge-webapp/src/main/resources/data/init-test-data-new.xml" });
  }
}
