package org.projectforge.framework.persistence.jpa;

import java.util.List;

import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

public class BasicJpaTest extends AbstractTestBase
{
  @Autowired
  private PfEmgrFactory emf;

  @Test
  public void testJpa()
  {
    List<PFUserDO> res = emf.runInTrans((emgr) -> {
      List<PFUserDO> ret = emgr.selectDetached(PFUserDO.class, "select e from " + PFUserDO.class.getName() + " e");
      return ret;
    });
  }
}
