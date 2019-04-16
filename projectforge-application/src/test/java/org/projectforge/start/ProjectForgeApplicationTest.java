package org.projectforge.start;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProjectForgeApplicationTest {
  static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjectForgeApplicationTest.class);

  @Test
  public void addDefaultAdditionalLocation() {
    String loc = ProjectForgeApplication.getAddtionalLocationArg();
    if (ProjectForgeApplication.addDefaultAdditionalLocation(null) == null) {
      log.warn("Found application{-default}.properties in current working directory (you should move it to ~/ProjectForge/projectforge.properties). Can't process with this text (OK).");
      return;
    }
    checkArray(new String[]{loc}, null);
    checkArray(new String[]{loc}, new String[]{});
    checkArray(new String[]{"spring.datasource.driver-class-name=org.postgresql.Driver", loc}, new String[]{"spring.datasource.driver-class-name=org.postgresql.Driver"});
    checkArray(new String[]{"--spring.config.additional-location=file:/opt/projectforge/test.properties"}, new String[]{"--spring.config.additional-location=file:/opt/projectforge/test.properties"});
    checkArray(new String[]{"hurzel", "--spring.config.additional-location=file:/opt/projectforge/test.properties"}, new String[]{"hurzel", "--spring.config.additional-location=file:/opt/projectforge/test.properties"});
  }

  private void checkArray(String[] expected, String[] array) {
    String[] args = ProjectForgeApplication.addDefaultAdditionalLocation(array);
    assertEquals(expected.length, args.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], args[i]);
    }
  }
}