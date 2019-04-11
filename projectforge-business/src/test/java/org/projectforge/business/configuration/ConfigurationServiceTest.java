package org.projectforge.business.configuration;

import static org.junit.jupiter.api.Assertions.*;

import org.projectforge.framework.configuration.ConfigurationDao;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

public class ConfigurationServiceTest extends AbstractTestBase
{
  @Autowired
  private ConfigurationService configurationService;

  @Autowired
  private ConfigurationDao configurationDao;

  @Test
  public void testGetMinPasswordLength()
  {
    final int defaultMinPwLen = ConfigurationParam.MIN_PASSWORD_LENGTH.getDefaultIntValue();

    // default
    assertEquals(defaultMinPwLen, configurationService.getMinPasswordLength());

    final ConfigurationDO minPwLenEntry = configurationDao.getEntry(ConfigurationParam.MIN_PASSWORD_LENGTH);
    minPwLenEntry.setIntValue(16);
    configurationDao.internalUpdate(minPwLenEntry);
    assertEquals(16, configurationService.getMinPasswordLength());

    // null -> use default
    minPwLenEntry.setIntValue(null);
    configurationDao.internalUpdate(minPwLenEntry);
    assertEquals(defaultMinPwLen, configurationService.getMinPasswordLength());
  }

  /**
   * Test flag password change verification on newly entered password, that passwords have to change.
   */
  @Test
  public void testGetFlagPasswordChange()
  {
    final boolean defaultFlagPwChange = ConfigurationParam.PASSWORD_FLAG_CHECK_CHANGE.getDefaultBooleanValue();

    // default
    assertEquals(defaultFlagPwChange, configurationService.getFlagCheckPasswordChange());

    final ConfigurationDO flagPwChange = configurationDao.getEntry(ConfigurationParam.PASSWORD_FLAG_CHECK_CHANGE);
    flagPwChange.setBooleanValue(false);
    configurationDao.internalUpdate(flagPwChange);
    assertEquals(false, configurationService.getFlagCheckPasswordChange());
  }
}
