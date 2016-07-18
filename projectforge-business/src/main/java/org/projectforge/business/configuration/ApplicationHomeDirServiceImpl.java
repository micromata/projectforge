package org.projectforge.business.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("applicationHomeDirService")
public class ApplicationHomeDirServiceImpl implements ApplicationHomeDirService
{
  @Value("${projectforge.base.dir}")
  private String applicationHomeDir;

  @Override
  public String getApplicationHomeDir()
  {
    return applicationHomeDir;
  }

}
