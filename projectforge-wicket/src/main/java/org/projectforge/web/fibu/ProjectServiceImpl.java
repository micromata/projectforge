package org.projectforge.web.fibu;

import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by blumenstein on 06.01.17.
 */
@Service
public class ProjectServiceImpl
{
  @Autowired
  private ProjektDao projektDao;

  public boolean isNumberFreeForCustomer(Integer numberValue, KundeDO customerValue)
  {
    return projektDao.getProjekt(customerValue, numberValue) == null;
  }
}
