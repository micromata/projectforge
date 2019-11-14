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
import org.projectforge.Const;
import org.projectforge.framework.i18n.I18nHelper;

import java.util.Locale;

/**
 * Bridge for hibernate search to search for payment type of incomming invoices.
 *
 * @author Stefan Niemczyk (s.niemczyk@micromata.de)
 */
public class HibernateSearchPaymentTypeBridge implements FieldBridge
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(HibernateSearchPaymentTypeBridge.class);

  /**
   * @see org.hibernate.search.bridge.FieldBridge#set(java.lang.String, java.lang.Object,
   * org.apache.lucene.document.Document, org.hibernate.search.bridge.LuceneOptions)
   */
  @Override
  public void set(final String name, final Object value, final Document document, final LuceneOptions luceneOptions)
  {
    final PaymentType paymentType = (PaymentType) value;
    if (paymentType == null) {
      return;
    }

    final StringBuilder buf = new StringBuilder();

    for (final Locale locale : Const.I18NSERVICE_LANGUAGES) {
      final String localized = I18nHelper.getLocalizedMessage(locale, paymentType.getI18nKey());

      if (localized == null) {
        continue;
      }

      buf.append(localized + " ");
    }

    if (log.isDebugEnabled()) {
      log.debug(buf.toString());
    }

    luceneOptions.addFieldToDocument(name, buf.toString(), document);
  }
}
