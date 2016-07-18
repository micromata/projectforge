package org.projectforge.common.task;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.projectforge.common.task.TaskStatus.*;

public class TaskStatusTest
{
  @Test
  public void testGetTaskStatus()
  {

    Assert.assertEquals(getTaskStatus("N"), N);
    Assert.assertEquals(getTaskStatus("O"), O);
    Assert.assertEquals(getTaskStatus("C"), C);
    try {
      getTaskStatus("");
    } catch (UnsupportedOperationException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testIsIn()
  {
    Assert.assertTrue(N.isIn(O, C, N));
    Assert.assertFalse(O.isIn(C, N));
    Assert.assertFalse(C.isIn(new TaskStatus[] {}));
    try {
      Assert.assertFalse(C.isIn(null));
    } catch (NullPointerException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testGetKeyAndI18nKey()
  {
    String notOpened = "notOpened";
    Assert.assertEquals(N.getKey(), notOpened);
    String opened = "opened";
    Assert.assertEquals(O.getKey(), opened);
    String closed = "closed";
    Assert.assertEquals(C.getKey(), closed);

    String pre = "task.status.";
    Assert.assertEquals(N.getI18nKey(), pre + notOpened);
    Assert.assertEquals(O.getI18nKey(), pre + opened);
    Assert.assertEquals(C.getI18nKey(), pre + closed);
  }
}
