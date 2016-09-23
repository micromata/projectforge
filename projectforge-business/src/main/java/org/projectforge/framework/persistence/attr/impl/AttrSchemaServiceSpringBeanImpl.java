/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.framework.persistence.attr.impl;

import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;
import de.micromata.genome.db.jpa.tabattr.api.AttrSchema;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithConfigurableAttr;
import de.micromata.genome.db.jpa.tabattr.impl.AttrSchemaServiceBaseImpl;

;

/**
 * AttrService which loads configuration from Spring context.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
public class AttrSchemaServiceSpringBeanImpl extends AttrSchemaServiceBaseImpl
{

  private static transient final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(AttrSchemaServiceSpringBeanImpl.class);

  private Map<String, AttrSchema> attrSchemata;

  private String applicationDir;

  private ApplicationContext context;

  private static AttrSchemaServiceSpringBeanImpl INSTANCE;

  public static AttrSchemaServiceSpringBeanImpl get()
  {
    if (INSTANCE == null) {
      INSTANCE = new AttrSchemaServiceSpringBeanImpl();
    }
    return INSTANCE;
  }

  public void setApplicationDir(String applicationDir)
  {
    this.applicationDir = applicationDir;
  }

  protected void loadAttrSchema()
  {
    String location = "file:" + applicationDir + "/attrschema.xml";
    try {
      context = new FileSystemXmlApplicationContext(location);
      attrSchemata = context.getBean("attrSchemataMap", Map.class);
    } catch (Exception e) {
      log.info("Can't load/parse AttrSchema config file. Message: " + e.getMessage());
    }
  }

  /**
   * @see org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService#getAttrSchema(java.lang.String)
   */
  @Override
  public AttrSchema getAttrSchema(final String name)
  {
    if (attrSchemata == null) {
      loadAttrSchema();
    }
    if (attrSchemata != null) {
      return attrSchemata.get(name);
    } else {
      return null;
    }
  }

  public void setAttrSchemata(Map<String, AttrSchema> attrSchemata)
  {
    this.attrSchemata = attrSchemata;
  }

  @Override
  public AttrGroup getAttrGroup(EntityWithConfigurableAttr entity, String groupName)
  {
    if (attrSchemata == null) {
      loadAttrSchema();
    }
    AttrSchema entitySchema = attrSchemata.get(entity.getAttrSchemaName());
    return entitySchema.getGroups().stream().filter(group -> group.getName().equals(groupName)).findFirst()
        .orElse(null);
  }

}
