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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.projectforge.framework.cache.AbstractCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.stereotype.Component;

/**
 * Caches the order positions assigned to invoice positions.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class RechnungCache extends AbstractCache
{
  private static Logger log = LoggerFactory.getLogger(RechnungCache.class);

  @Autowired
  private HibernateTemplate hibernateTemplate;

  /** The key is the order id. */
  private Map<Integer, Set<RechnungsPositionVO>> invoicePositionMapByAuftragId;

  /** The key is the order position id. */
  private Map<Integer, Set<RechnungsPositionVO>> invoicePositionMapByAuftragsPositionId;

  public Set<RechnungsPositionVO> getRechnungsPositionVOSetByAuftragId(final Integer auftragId)
  {
    checkRefresh();
    return invoicePositionMapByAuftragId.get(auftragId);
  }

  public Set<RechnungsPositionVO> getRechnungsPositionVOSetByAuftragsPositionId(final Integer auftragsPositionId)
  {
    checkRefresh();
    return invoicePositionMapByAuftragsPositionId.get(auftragsPositionId);
  }

  /**
   * This method will be called by CacheHelper and is synchronized via getData();
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void refresh()
  {
    log.info("Initializing RechnungCache ...");
    // This method must not be synchronized because it works with a new copy of maps.
    final Map<Integer, Set<RechnungsPositionVO>> mapByAuftragId = new HashMap<Integer, Set<RechnungsPositionVO>>();
    final Map<Integer, Set<RechnungsPositionVO>> mapByAuftragsPositionId = new HashMap<Integer, Set<RechnungsPositionVO>>();
    final List<RechnungsPositionDO> list = (List<RechnungsPositionDO>) hibernateTemplate.find(
        "from RechnungsPositionDO t left join fetch t.auftragsPosition left join fetch t.auftragsPosition.auftrag where t.auftragsPosition is not null");
    for (final RechnungsPositionDO pos : list) {
      if (pos.getAuftragsPosition() == null || pos.getAuftragsPosition().getAuftrag() == null) {
        log.error("Assigned order position expected: " + pos);
        continue;
      } else if (pos.isDeleted() == true || pos.getRechnung() == null || pos.getRechnung().isDeleted() == true
          || pos.getRechnung().getNummer() == null) {
        // Invoice position or invoice is deleted.
        continue;
      }
      final AuftragsPositionDO auftragsPosition = pos.getAuftragsPosition();
      final AuftragDO auftrag = auftragsPosition.getAuftrag();
      Set<RechnungsPositionVO> setByAuftragId = mapByAuftragId.get(auftrag.getId());
      if (setByAuftragId == null) {
        setByAuftragId = new TreeSet<RechnungsPositionVO>();
        mapByAuftragId.put(auftrag.getId(), setByAuftragId);
      }
      Set<RechnungsPositionVO> setByAuftragsPositionId = mapByAuftragsPositionId.get(auftragsPosition.getId());
      if (setByAuftragsPositionId == null) {
        setByAuftragsPositionId = new TreeSet<RechnungsPositionVO>();
        mapByAuftragsPositionId.put(auftragsPosition.getId(), setByAuftragsPositionId);
      }
      final RechnungsPositionVO vo = new RechnungsPositionVO(pos);
      if (setByAuftragId.contains(vo) == false) {
        setByAuftragId.add(vo);
      }
      if (setByAuftragsPositionId.contains(vo) == false) {
        setByAuftragsPositionId.add(vo);
      }
    }
    this.invoicePositionMapByAuftragId = mapByAuftragId;
    this.invoicePositionMapByAuftragsPositionId = mapByAuftragsPositionId;
    log.info("Initializing of RechnungCache done.");
  }

}
