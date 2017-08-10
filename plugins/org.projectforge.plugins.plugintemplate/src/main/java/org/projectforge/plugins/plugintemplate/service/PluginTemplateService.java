package org.projectforge.plugins.plugintemplate.service;

import org.projectforge.plugins.plugintemplate.repository.PluginTemplateDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PluginTemplateService
{
  @Autowired
  private PluginTemplateDao pluginTemplateDao;

  public PluginTemplateDao getPluginTemplateDao()
  {
    return pluginTemplateDao;
  }
}
