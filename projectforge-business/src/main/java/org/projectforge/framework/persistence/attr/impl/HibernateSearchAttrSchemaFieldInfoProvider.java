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

package org.projectforge.framework.persistence.attr.impl;

import java.util.HashMap;
import java.util.Map;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;
import de.micromata.genome.db.jpa.tabattr.api.AttrSchema;
import de.micromata.genome.db.jpa.tabattr.api.AttrSchemaService;
import de.micromata.genome.jpa.metainf.ColumnMetadata;
import de.micromata.genome.jpa.metainf.ColumnMetadataBean;
import de.micromata.genome.jpa.metainf.EntityMetadata;
import de.micromata.mgc.jpa.hibernatesearch.api.HibernateSearchFieldInfoProvider;
import de.micromata.mgc.jpa.hibernatesearch.api.SearchColumnMetadata;
import de.micromata.mgc.jpa.hibernatesearch.impl.SearchColumnMetadataBean;

/**
 * Add the configured attr schema to search fields.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class HibernateSearchAttrSchemaFieldInfoProvider implements HibernateSearchFieldInfoProvider
{

  private static transient final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(HibernateSearchAttrSchemaFieldInfoProvider.class);

  private ColumnMetadata getColumnMetadataFromColumnDesc(EntityMetadata entm, AttrDescription ad)
  {
    ColumnMetadataBean mb = new ColumnMetadataBean(entm);
    mb.setName(ad.getPropertyName());
    mb.setJavaType(String.class);
    // TODO RK more fields
    return mb;
  }

  @Override
  public Map<String, SearchColumnMetadata> getAdditionallySearchFields(EntityMetadata entm,
      String params)
  {
    Map<String, SearchColumnMetadata> ret = new HashMap<>();

    AttrSchemaService service = AttrSchemaServiceSpringBeanImpl.get();
    AttrSchema schema = service.getAttrSchema(params);
    if (schema != null) {
      for (AttrGroup group : schema.getGroups()) {
        for (AttrDescription ad : group.getDescriptions()) {
          ColumnMetadata cmd = getColumnMetadataFromColumnDesc(entm, ad);
          SearchColumnMetadataBean scmd = new SearchColumnMetadataBean(ad.getPropertyName(), cmd);
          scmd.setIndexType(String.class);
          scmd.setIndexed(true);
          ret.put(ad.getPropertyName(), scmd);
        }
      }
      return ret;
    } else {
      log.info("Can't get AttrSchema. Continoue without.");
      return null;
    }
  }

}
