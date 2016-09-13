package org.projectforge.business.teamcal.service;

import java.util.Map;

public interface CryptService
{

  Map<String, String> decryptParameterMessage(String message);

  String encryptParameterMessage(String message);

}
