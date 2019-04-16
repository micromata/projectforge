package org.projectforge.common.task;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.projectforge.common.task.TaskStatus.*;

public class TaskStatusTest
{
  @Test
  public void testGetTaskStatus()
  {

    Assertions.assertEquals(getTaskStatus("N"), N);
    Assertions.assertEquals(getTaskStatus("O"), O);
    Assertions.assertEquals(getTaskStatus("C"), C);
    try {
      getTaskStatus("");
    } catch (UnsupportedOperationException e) {
      Assertions.assertNotNull(e);
    }
  }

  @Test
  public void testIsIn()
  {
    Assertions.assertTrue(N.isIn(O, C, N));
    Assertions.assertFalse(O.isIn(C, N));
    Assertions.assertFalse(C.isIn(new TaskStatus[] {}));
    try {
      Assertions.assertFalse(C.isIn(null));
    } catch (NullPointerException e) {
      Assertions.assertNotNull(e);
    }
  }

  @Test
  public void testGetKeyAndI18nKey()
  {
    String notOpened = "notOpened";
    Assertions.assertEquals(N.getKey(), notOpened);
    String opened = "opened";
    Assertions.assertEquals(O.getKey(), opened);
    String closed = "closed";
    Assertions.assertEquals(C.getKey(), closed);

    String pre = "task.status.";
    Assertions.assertEquals(N.getI18nKey(), pre + notOpened);
    Assertions.assertEquals(O.getI18nKey(), pre + opened);
    Assertions.assertEquals(C.getI18nKey(), pre + closed);
  }
}
