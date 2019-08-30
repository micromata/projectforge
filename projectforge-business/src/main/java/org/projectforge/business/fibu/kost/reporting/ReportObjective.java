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

package org.projectforge.business.fibu.kost.reporting;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Ein Reportobjective, dessen Properties aus einem XML-File geparst werden können. Die Beschreibung des Formats ist im Handbuch zu finden.
 * Ein ReportObjective ist einem Report zugeordnet. Der Report nutzt das ReportObjective, um die Buchungssätze zu filtern. <br/> ACHTUNG:
 * Ein Child-ReportObjective kann die Buchungssätze des Eltern-ReportObjectives immer nur subselektieren, d. h. die Buchungssätze bilden
 * immer eine Untermenge!
 * @author Thomas Landgraf (tl@micromata.de)
 * @author Kai Reinhard
 *
 */
@XStreamAlias("ReportObjective")
public class ReportObjective
{
  @XStreamAsAttribute
  private String title;

  @XStreamAsAttribute
  private String id;

  @XStreamAsAttribute
  private boolean suppressOther;

  @XStreamAsAttribute
  private boolean suppressDuplicates;

  @XStreamImplicit(itemFieldName = "kost1-include")
  private List<String> kost1IncludeRegExpList;

  @XStreamImplicit(itemFieldName = "kost1-exclude")
  private List<String> kost1ExcludeRegExpList;

  @XStreamImplicit(itemFieldName = "kost2-include")
  private List<String> kost2IncludeRegExpList;

  @XStreamImplicit(itemFieldName = "kost2-exclude")
  private List<String> kost2ExcludeRegExpList;

  @XStreamImplicit(itemFieldName = "ReportObjective")
  private List<ReportObjective> childReportObjectives;

  private transient ReportObjective parent;

  public ReportObjective()
  {
  }

  /**
   * Klartexttitel
   */
  public String getTitle()
  {
    return title;
  }

  public void setTitle(String title)
  {
    this.title = title;
  }

  /**
   * Eine fast Klartext ID z.B. "projekt4711" zum Basteln der Hyperlink- bzw. Ordner-Struktur
   */
  public String getId()
  {
    return id;
  }

  public void setId(String id)
  {
    this.id = id;
  }

  /**
   * @return False (default), wenn die sonstigen Buchungssätze als eigenes Child-ReportObjective berücksichtigt werden sollen. Es werden die
   *         Child-ReportObjectives untersucht und von den Children nicht verwendete Buchungssätze dieses ReportObjectives zusammengestellt.
   *         Die Summe aller Buchungssätze / BWAs der Children sollte somit dieser ReportObjective entsprechen (falls keine Duplikate
   *         vorkommen).
   * @see #isSupressDuplicates()
   */
  public boolean isSuppressOther()
  {
    return suppressOther;
  }

  public void setSuppressOther(boolean suppressOther)
  {
    this.suppressOther = suppressOther;
  }

  /**
   * @return False (default), wenn Dupletten, d. h. mehrfach von den Child-ReportObjectives verwendete Buchungssätze, als eigenes
   *         Child-ReportObjective zusammengefasst werden sollen. Über dieses Flag und isOther kann somit die Vollständigkeit und
   *         Duplettenfreiheit von Child-ReportObjectives geprüft werden.
   * @see #isSuppressOther()
   */
  public boolean isSuppressDuplicates()
  {
    return suppressDuplicates;
  }

  public void setSuppressDuplicates(boolean suppressDuplicates)
  {
    this.suppressDuplicates = suppressDuplicates;
  }

  /**
   * @return True, wenn Child-ReportObjectives existieren, sonst false.
   */
  public boolean getHasChildren()
  {
    return CollectionUtils.isNotEmpty(this.childReportObjectives);
  }

  /**
   * @deprecated
   */
  public boolean getHasChilds()
  {
    return getHasChildren();
  }

  /**
   * Ein ReportObjective als Childobjective zuordnen. Der Nutzer hat so die Möglichkeit einen Drilldown zu betreiben.
   */
  public void addChildReportObjective(ReportObjective childReportObjective)
  {
    if (childReportObjectives == null) {
      childReportObjectives = new ArrayList<ReportObjective>();
    }
    childReportObjectives.add(childReportObjective);
    childReportObjective.parent = this;
  }

  /**
   *
   * @return Liste der Childreports, die einen Drilldown dieses Reports darstellen... oder null, wenn der Report keine Childreports besitzt.
   */
  public List<ReportObjective> getChildReportObjectives()
  {
    return this.childReportObjectives;
  }

  public List<String> getKost1IncludeRegExpList()
  {
    return kost1IncludeRegExpList;
  }

  public List<String> getKost1ExcludeRegExpList()
  {
    return kost1ExcludeRegExpList;
  }

  public List<String> getKost2IncludeRegExpList()
  {
    return kost2IncludeRegExpList;
  }

  public List<String> getKost2ExcludeRegExpList()
  {
    return kost2ExcludeRegExpList;
  }

  /**
   * Wenn die Liste leer ist, so werden alle Kost1-Einträge angenommen. Ansonsten kann ein Eintrag nur angenommen werden, wenn er mindestens
   * mit einem dieser regulären Ausdrücke matcht.<br/> The syntax of the regExp is described in class Pattern.
   * @param regExp
   * @see java.util.regex.Pattern
   * @see Report#match(List, String, boolean)
   */
  public void addKost1IncludeRegExp(String regExp)
  {
    this.kost1IncludeRegExpList = addRegExp(this.kost1IncludeRegExpList, regExp);
  }

  /**
   * Sobald ein solcher Eintrag matcht, wird unabhängig von den Whitelisten dieser Eintrag ignoriert. Ansonsten kann ein Eintrag nur
   * angenommen werden, wenn er mindestens mit einem dieser regulären Ausdrücke matcht.<br/> The syntax of the regExp is described in class
   * Pattern.
   * @param regExp
   * @see java.util.regex.Pattern
   * @see Report#match(List, String, boolean)
   */
  public void addKost1ExcludeRegExp(String regExp)
  {
    this.kost1ExcludeRegExpList = addRegExp(this.kost1ExcludeRegExpList, regExp);
  }

  /**
   * Wenn die Liste leer ist, so werden alle Kost2-Einträge angenommen.<br/> The syntax of the regExp is described in class Pattern.
   * @param regExp
   * @see java.util.regex.Pattern
   * @see Report#match(List, String, boolean)
   */
  public void addKost2IncludeRegExp(String regExp)
  {
    this.kost2IncludeRegExpList = addRegExp(this.kost2IncludeRegExpList, regExp);
  }

  /**
   * Sobald ein solcher Eintrag matcht, wird unabhängig von den Whitelisten dieser Eintrag ignoriert.<br/> The syntax of the regExp is
   * described in class Pattern.
   * @param regExp
   * @see java.util.regex.Pattern
   * @see Report#match(List, String, boolean)
   */
  public void addKost2ExcludeRegExp(String regExp)
  {
    this.kost2ExcludeRegExpList = addRegExp(this.kost2ExcludeRegExpList, regExp);
  }

  public ReportObjective getParent()
  {
    return parent;
  }

  private List<String> addRegExp(List<String> regExpList, String regExp)
  {
    if (regExpList == null) {
      regExpList = new ArrayList<String>();
    }
    regExpList.add(regExp);
    return regExpList;
  }
}
