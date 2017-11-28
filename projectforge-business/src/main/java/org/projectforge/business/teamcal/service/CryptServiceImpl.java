package org.projectforge.business.teamcal.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.framework.utils.Crypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CryptServiceImpl implements CryptService
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CryptServiceImpl.class);

  @Autowired
  private ConfigurationService configService;

  @Override
  public Map<String, String> decryptParameterMessage(String message)
  {
    Map<String, String> result = new HashMap<>();
    String decryptMessage = Crypt.decrypt(StringUtils.rightPad(configService.getTeamCalCryptPassword(), 32, "x"),
        message);
    if (decryptMessage != null) {
      String[] parametersAndValues = decryptMessage.split("&");
      for (String parameterWithValue : parametersAndValues) {
        String[] parameterAndValue = parameterWithValue.split("=");
        if (parameterAndValue.length > 1) {
          result.put(parameterAndValue[0], parameterAndValue[1]);
        }
      }
    }
    return result;
  }

  @Override
  public String encryptParameterMessage(String message)
  {
    return Crypt.encrypt(StringUtils.rightPad(configService.getTeamCalCryptPassword(), 32, "x"), message);
  }

}
