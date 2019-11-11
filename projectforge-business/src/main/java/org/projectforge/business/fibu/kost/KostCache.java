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

package org.projectforge.business.fibu.kost;

import org.apache.commons.collections.CollectionUtils;
import org.projectforge.framework.cache.AbstractCache;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.reporting.Kost2Art;
import org.projectforge.reporting.impl.Kost2ArtImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.util.*;

/**
 * The kost2 entries will be cached.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class KostCache extends AbstractCache {
  private static Logger log = LoggerFactory.getLogger(KostCache.class);

  @Autowired
  private EntityManager em;

  /**
   * The key is the kost2-id.
   */
  private Map<Integer, Kost2DO> kost2Map;

  /**
   * The key is the kost2-id.
   */
  private Map<Integer, Kost1DO> kost1Map;

  private List<Kost2Art> allKost2Arts;

  private boolean kost2EntriesExists = false;

  public Kost2DO getKost2(final Integer kost2Id) {
    if (!NumberHelper.greaterZero(kost2Id)) {
      return null;
    }
    return getKost2Map().get(kost2Id);
  }

  /**
   * @param kostString Format ######## or #.###.##.## is supported.
   * @see #getKost2(int, int, int, int)
   */
  public Kost2DO getKost2(final String kostString) {
    final int[] kost = KostHelper.parseKostString(kostString);
    if (kost == null) {
      return null;
    }
    return getKost2(kost[0], kost[1], kost[2], kost[3]);
  }

  public Kost2DO getKost2(final int nummernkreis, final int bereich, final int teilbereich, final int kost2art) {
    for (final Kost2DO kost : getKost2Map().values()) {
      if (kost.getNummernkreis() == nummernkreis
              && kost.getBereich() == bereich
              && kost.getTeilbereich() == teilbereich
              && kost.getKost2ArtId() == kost2art) {
        return kost;
      }
    }
    return null;
  }

  public List<Kost2DO> getActiveKost2(final int nummernkreis, final int bereich, final int teilbereich) {
    final List<Kost2DO> list = new ArrayList<>();
    for (final Kost2DO kost : getKost2Map().values()) {
      if (kost.getNummernkreis() == nummernkreis
              && kost.getBereich() == bereich
              && kost.getTeilbereich() == teilbereich
              && (kost.getKostentraegerStatus() == KostentraegerStatus.ACTIVE || kost.getKostentraegerStatus() == null)) {
        list.add(kost);
      }
    }
    if (CollectionUtils.isEmpty(list)) {
      return null;
    }
    return list;
  }

  public Kost1DO getKost1(final Integer kost1Id) {
    if (!NumberHelper.greaterZero(kost1Id)) {
      return null;
    }
    return getKost1Map().get(kost1Id);
  }

  /**
   * @param kostString Format ######## or #.###.##.## is supported.
   * @see #getKost2(int, int, int, int)
   */
  public Kost1DO getKost1(final String kostString) {
    final int[] kost = KostHelper.parseKostString(kostString);
    for (final Kost1DO kost1 : getKost1Map().values()) {
      if (kost[0] == kost1.getNummernkreis() &&
              kost[1] == kost1.getBereich() &&
              kost[2] == kost1.getTeilbereich() &&
              kost[3] == kost1.getEndziffer()) {
        return kost1;
      }
    }
    return null;
  }

  /**
   * Gibt die für das Projekt definierten, nicht gelöschten Kostenarten zurück.
   *
   * @param projektId
   */
  public Set<Kost2ArtDO> getKost2Arts(final Integer projektId) {
    checkRefresh();
    final Set<Kost2ArtDO> set = new TreeSet<>();
    if (projektId == null) {
      return set;
    }
    for (final Kost2DO kost : getKost2Map().values()) {
      if (kost.isDeleted()) {
        continue;
      }
      if (Objects.equals(projektId, kost.getProjektId())) {
        final Kost2ArtDO kost2Art = kost.getKost2Art();
        if (kost2Art != null) {
          set.add(kost2Art);
        }
      }
    }
    return set;
  }

  /**
   * Gibt alle nicht gelöschten Kostenarten zurück, wobei die für das Projekt definierten entsprechend markiert sind.
   *
   * @param projektId
   */
  public List<Kost2Art> getAllKost2Arts(final Integer projektId) {
    checkRefresh();
    final Set<Kost2ArtDO> set = getKost2Arts(projektId);
    final List<Kost2Art> result = new ArrayList<>();
    for (final Kost2Art kost2Art : allKost2Arts) {
      if (kost2Art.isDeleted()) {
        continue;
      }
      final Kost2ArtDO kost2ArtDO = new Kost2ArtDO();
      kost2ArtDO.copyValuesFrom(((Kost2ArtImpl) kost2Art).getKost2ArtDO());
      final Kost2ArtImpl art = new Kost2ArtImpl(kost2ArtDO);
      if (set.contains(((Kost2ArtImpl) kost2Art).getKost2ArtDO())) {
        art.setExistsAlready(true);
      }
      result.add(art);
    }
    return result;
  }

  public List<Kost2Art> getAllKostArts() {
    checkRefresh();
    final List<Kost2Art> list = new ArrayList<>();
    if (allKost2Arts != null) {
      for (final Kost2Art kost2Art : allKost2Arts) {
        final Kost2ArtDO kost2ArtDO = ((Kost2ArtImpl) kost2Art).getKost2ArtDO();
        final Kost2ArtDO clone = new Kost2ArtDO();
        clone.copyValuesFrom(kost2ArtDO);
        list.add(new Kost2ArtImpl(clone));
      }
    }
    return list;
  }

  public boolean isKost2EntriesExists() {
    checkRefresh();
    return kost2EntriesExists;
  }

  /**
   * Should be called after user modifications.
   */
  void updateKost2(final Kost2DO kost2) {
    getKost2Map().put(kost2.getId(), kost2);
  }

  /**
   * Should be called after user modifications.
   */
  void updateKost1(final Kost1DO kost1) {
    getKost1Map().put(kost1.getId(), kost1);
  }

  void updateKost2Arts() {
    List<Kost2ArtDO> result = em.createQuery("from Kost2ArtDO t where t.deleted = false order by t.id",
            Kost2ArtDO.class)
            .setLockMode(LockModeType.NONE)
            .getResultList();
    final List<Kost2Art> list = new ArrayList<>();
    for (final Kost2ArtDO kost2ArtDO : result) {
      final Kost2ArtImpl art = new Kost2ArtImpl(kost2ArtDO);
      list.add(art);
    }
    // This method must not be synchronized because it works with a new copy of list.
    this.allKost2Arts = list;
  }

  private Map<Integer, Kost2DO> getKost2Map() {
    checkRefresh();
    return kost2Map;
  }

  private Map<Integer, Kost1DO> getKost1Map() {
    checkRefresh();
    return kost1Map;
  }

  /**
   * This method will be called by CacheHelper and is synchronized via getData();
   */
  @Override
  protected void refresh() {
    log.info("Initializing KostCache ...");
    // This method must not be synchronized because it works with a new copy of maps.
    final Map<Integer, Kost1DO> map1 = new HashMap<>();
    final List<Kost1DO> list1 = em.createQuery("from Kost1DO t", Kost1DO.class)
            .setLockMode(LockModeType.NONE)
            .getResultList();
    for (final Kost1DO kost1 : list1) {
      map1.put(kost1.getId(), kost1);
    }
    this.kost1Map = map1;
    final Map<Integer, Kost2DO> map2 = new HashMap<>();
    final List<Kost2DO> list2 = em.createQuery("from Kost2DO t", Kost2DO.class)
            .setLockMode(LockModeType.NONE)
            .getResultList();
    kost2EntriesExists = false;
    for (final Kost2DO kost2 : list2) {
      if (!kost2EntriesExists && !kost2.isDeleted()) {
        kost2EntriesExists = true;
      }
      map2.put(kost2.getId(), kost2);
    }
    this.kost2Map = map2;
    updateKost2Arts();
    log.info("Initializing of KostCache done.");
  }
}
