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

package org.projectforge.business.fibu;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * Bridge for hibernate search to search for order positions of form ###.## (&lt;order number&gt;.&lt;position
 * number&gt>).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class HibernateSearchAuftragsPositionBridge implements FieldBridge
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(HibernateSearchAuftragsPositionBridge.class);

  /**
   * @see org.hibernate.search.bridge.FieldBridge#set(java.lang.String, java.lang.Object,
   *      org.apache.lucene.document.Document, org.hibernate.search.bridge.LuceneOptions)
   */
  @Override
  public void set(final String name, final Object value, final Document document, final LuceneOptions luceneOptions)
  {
    final AuftragsPositionDO position = (AuftragsPositionDO) value;
    if (position == null) {
      log.error("AuftragsPositionDO object is null.");
      return;
    }
    final AuftragDO auftrag = position.getAuftrag();
    final StringBuffer buf = new StringBuffer();
    if (auftrag == null || auftrag.getNummer() == null) {
      log.error("AuftragDO for AuftragsPositionDO: " + position.getId() + "  is null.");
      return;
    }
    buf.append(auftrag.getNummer()).append(".").append(position.getNumber());
    if (log.isDebugEnabled() == true) {
      log.debug(buf.toString());
    }
    luceneOptions.addFieldToDocument(name, buf.toString(), document);
  }
}
