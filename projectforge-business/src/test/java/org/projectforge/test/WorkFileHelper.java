package org.projectforge.test;

import java.io.File;

public class WorkFileHelper
{

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WorkFileHelper.class);

  private static final String WORK_DIR = "./target/work";

  /**
   * Get the file from the working directory. If the working directory doesn't exist then it'll be created.
   * 
   * @param filename
   * @return
   */
  public static File getWorkFile(final String filename)
  {
    final File workDir = new File(WORK_DIR);
    if (workDir.exists() == false) {
      log.info("Create working directory: " + workDir.getAbsolutePath());
      workDir.mkdir();
    }
    return new File(workDir, filename);
  }

}
