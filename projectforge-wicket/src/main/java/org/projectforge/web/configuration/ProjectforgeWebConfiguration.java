package org.projectforge.web.configuration;

import java.util.HashMap;
import java.util.Map;

import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService;
import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaServiceImpl;
import org.projectforge.renderer.custom.Formatter;
import org.projectforge.renderer.custom.FormatterFactory;
import org.projectforge.renderer.custom.MicromataFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProjectforgeWebConfiguration
{
  @Value("${projectforge.base.dir}")
  private String applicationDir;

  @Autowired
  private ApplicationContext applicationContext;

  @Bean
  public GuiAttrSchemaService guiAttrSchemaService()
  {
    GuiAttrSchemaServiceImpl ret = new GuiAttrSchemaServiceImpl();
    ret.setApplicationDir(applicationDir);
    return ret;
  }

  @Bean
  public FormatterFactory formatterFactory()
  {
    FormatterFactory fac = new FormatterFactory();
    Map<String, Formatter> formatters = new HashMap<>();
    formatters.put("Micromata", applicationContext.getBean(MicromataFormatter.class));
    fac.setFormatters(formatters);
    return fac;
  }

}
