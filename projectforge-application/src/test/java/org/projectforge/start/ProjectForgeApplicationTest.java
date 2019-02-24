package org.projectforge.start;


import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

public class ProjectForgeApplicationTest {
  @Test
  public void addDefaultAdditionalLocation() {
    String loc = ProjectForgeApplication.getAddtionalLocationArg();
    checkArray(new String[] {loc}, null);
    checkArray(new String[] {loc}, new String[]{});
    checkArray(new String[] {"spring.datasource.driver-class-name=org.postgresql.Driver", loc}, new String[]{"spring.datasource.driver-class-name=org.postgresql.Driver"});
    checkArray(new String[] {"--spring.config.additional-location=file:/opt/projectforge/test.properties"}, new String[]{"--spring.config.additional-location=file:/opt/projectforge/test.properties"});
    checkArray(new String[] {"hurzel", "--spring.config.additional-location=file:/opt/projectforge/test.properties"}, new String[]{"hurzel", "--spring.config.additional-location=file:/opt/projectforge/test.properties"});
  }

  private void checkArray(String[] expected, String[] array) {
    String[] args = ProjectForgeApplication.addDefaultAdditionalLocation(array);
    assertEquals(expected.length, args.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], args[i]);
    }
  }
}