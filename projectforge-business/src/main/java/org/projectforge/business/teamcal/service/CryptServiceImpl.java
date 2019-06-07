/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

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
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CryptServiceImpl.class);

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
