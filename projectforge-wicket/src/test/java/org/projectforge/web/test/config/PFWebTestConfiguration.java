/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.test.config;

import de.micromata.genome.db.jpa.tabattr.api.TimeableService;
import de.micromata.genome.db.jpa.tabattr.impl.TimeableServiceImpl;
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
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;
import java.util.Map;

@Configuration
@PropertySource({"classpath:/application.properties", "classpath:/application-test.properties"})
public class PFWebTestConfiguration
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
  public TimeableService timeableService()
  {
    return new TimeableServiceImpl();
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
