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

package org.projectforge.business.fibu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;
import org.hibernate.LazyInitializationException;
import org.projectforge.framework.cache.AbstractCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.stereotype.Component;

/**
 * Caches the DATEV accounts.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class KontoCache extends AbstractCache
{
  private static Logger log = Logger.getLogger(KontoCache.class);

  @Autowired
  private HibernateTemplate hibernateTemplate;

  /** The key is the order id. */
  private Map<Integer, KontoDO> accountMapById;

  public boolean isEmpty()
  {
    checkRefresh();
    return MapUtils.isEmpty(accountMapById);
  }

  public KontoDO getKonto(final Integer id)
  {
    if (id == null) {
      return null;
    }
    checkRefresh();
    return accountMapById.get(id);
  }

  /**
   * Gets account of given project if given, otherwise the account assigned to the customer assigned to this project. If
   * no account is given at all, null is returned.<br/>
   * Please note: The object of project must be initialized including the assigned customer, if not a
   * {@link LazyInitializationException} could be thrown.
   * 
   * @param project
   * @return The assigned account if given, otherwise null.
   */
  public KontoDO getKonto(final ProjektDO project)
  {
    if (project == null) {
      return null;
    }
    checkRefresh();
    KontoDO konto = getKonto(project.getKontoId());
    if (konto != null) {
      return konto;
    }
    final KundeDO customer = project.getKunde();
    if (customer != null) {
      konto = getKonto(customer.getKontoId());
    }
    return konto;
  }

  /**
   * Gets account:
   * <ol>
   * <li>Returns the account of given invoice if given.</li>
   * <li>Returns the account of the assigned project if given.</li>
   * <li>Returns the account assigned to the customer of this invoice if given.</li>
   * <li>Returns the account of the customer assigned to the project if given.<br/>
   * Please note: The object of project must be initialized including the assigned customer, if not a
   * {@link LazyInitializationException} could be thrown.
   * 
   * @param invoice
   * @return The assigned account if given, otherwise null.
   */
  public KontoDO getKonto(final RechnungDO invoice)
  {
    if (invoice == null) {
      return null;
    }
    checkRefresh();
    KontoDO konto = getKonto(invoice.getKontoId());
    if (konto != null) {
      return konto;
    }
    final ProjektDO project = invoice.getProjekt();
    if (project != null) {
      konto = getKonto(project.getKontoId());
      if (konto != null) {
        return konto;
      }
    }
    KundeDO kunde = invoice.getKunde();
    if (kunde != null) {
      konto = getKonto(kunde.getKontoId());
    }
    if (konto != null) {
      return konto;
    }
    if (project != null) {
      kunde = project.getKunde();
      if (kunde != null) {
        konto = getKonto(kunde.getKontoId());
      }
    }
    return konto;
  }

  /**
   * This method will be called by CacheHelper and is synchronized via getData();
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void refresh()
  {
    log.info("Initializing KontoCache ...");
    // This method must not be synchronized because it works with a new copy of maps.
    final Map<Integer, KontoDO> map = new HashMap<Integer, KontoDO>();
    final List<KontoDO> list = (List<KontoDO>) hibernateTemplate.find("from KontoDO t where deleted=false");
    for (final KontoDO konto : list) {
      map.put(konto.getId(), konto);
    }
    this.accountMapById = map;
    log.info("Initializing of KontoCache done.");
  }

}
