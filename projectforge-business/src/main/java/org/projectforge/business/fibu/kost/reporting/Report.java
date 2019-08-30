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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.kost.AccountingConfig;
import org.projectforge.business.fibu.kost.BuchungssatzDO;
import org.projectforge.business.fibu.kost.BusinessAssessment;
import org.projectforge.business.fibu.kost.BusinessAssessmentTable;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Ein Report enthält unterliegende Buchungssätze, die gemäß Zeitraum und zugehörigem ReportObjective selektiert werden.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class Report implements Serializable
{
  private static final long serialVersionUID = -5359861335173843043L;

  private transient List<BuchungssatzDO> buchungssaetze;

  private transient Set<BuchungssatzDO> buchungssatzSet;

  private transient ReportObjective reportObjective;

  private transient List<Report> childReports;

  private transient List<BuchungssatzDO> other;

  private transient List<BuchungssatzDO> duplicates;

  private boolean showChildren;

  private transient BusinessAssessment businessAssessment;

  private transient BusinessAssessmentTable businessAssessmentTable;

  private int fromYear;

  private int fromMonth;

  private int toYear;

  private int toMonth;

  private transient Report parent;

  public Report(final ReportObjective reportObjective)
  {
    this.reportObjective = reportObjective;
  }

  public Report(final ReportObjective reportObjective, final Report parent)
  {
    this(reportObjective, parent.fromYear, parent.fromMonth, parent.toYear, parent.toMonth);
    this.parent = parent;
  }

  public Report(final ReportObjective reportObjective, final int fromYear, final int fromMonth, final int toYear, final int toMonth)
  {
    this(reportObjective);
    this.fromYear = fromYear;
    this.fromMonth = fromMonth;
    this.toYear = toYear;
    this.toMonth = toMonth;
  }

  public void setFrom(final int year, final int month)
  {
    this.fromYear = year;
    this.fromMonth = month;
  }

  public void setTo(final int year, final int month)
  {
    this.toYear = year;
    this.toMonth = month;
  }

  public int getFromYear()
  {
    return fromYear;
  }

  public int getFromMonth()
  {
    return fromMonth;
  }

  public int getToYear()
  {
    return toYear;
  }

  public int getToMonth()
  {
    return toMonth;
  }

  public Report getParent()
  {
    return parent;
  }

  /**
   * Gibt den Reportpfad zurück, vom Root-Report bis zum direkten Eltern-Report. Der Report selber ist nicht im Pfad enthalten.
   * @return Liste oder null, wenn der Report keinen Elternreport hat.
   */
  public List<Report> getPath()
  {
    if (this.parent == null) {
      return null;
    }
    final List<Report> path = new ArrayList<Report>();
    this.parent.getPath(path);
    return path;
  }

  private void getPath(final List<Report> path)
  {
    if (this.parent != null) {
      this.parent.getPath(path);
    }
    path.add(this);
  }

  public ReportObjective getReportObjective()
  {
    return reportObjective;
  }

  public BusinessAssessment getBusinessAssessment()
  {
    if (this.businessAssessment == null) {
      this.businessAssessment = new BusinessAssessment(AccountingConfig.getInstance().getBusinessAssessmentConfig());
      this.businessAssessment.setReference(this);
      this.businessAssessment.setStoreAccountRecordsInRows(true);
      this.businessAssessment.setAccountRecords(this.buchungssaetze);
    }
    return this.businessAssessment;
  }

  /**
   * Creates an array with all business assessment's of the child reports.
   * @param prependThisReport If true then the business assessment of this report will be prepend as first column.
   */
  public BusinessAssessmentTable getChildBusinessAssessmentTable(final boolean prependThisReport)
  {
    if (businessAssessmentTable == null) {
      if (prependThisReport == false && hasChildren() == false) {
        return null;
      }
      businessAssessmentTable = new BusinessAssessmentTable();
      if (prependThisReport == true) {
        businessAssessmentTable.addBusinessAssessment(this.getId(), this.getBusinessAssessment());
      }
      if (hasChildren() == true) {
        for (final Report child : getChildren()) {
          businessAssessmentTable.addBusinessAssessment(child.getId(), child.getBusinessAssessment());
        }
      }
    }
    return businessAssessmentTable;
  }

  /**
   * Wurde eine Selektion bereits durchgeführt?
   * @return true, wenn Buchungssätzeliste vorhanden ist (kann aber auf Grund der Selektion auch leer sein).
   */
  public boolean isLoad()
  {
    return this.buchungssaetze != null;
  }

  /**
   * @deprecated
   */
  public boolean isShowChilds()
  {
    return isShowChildren();
  }

  public boolean isShowChildren()
  {
    return showChildren;
  }

  public void setShowChildren(final boolean showChildren)
  {
    this.showChildren = showChildren;
  }

  /**
   * @see ReportObjective#getHasChildren()
   */
  public boolean hasChildren()
  {
    return reportObjective.getHasChildren();
  }

  /**
   * @deprecated
   */
  public boolean hasChilds()
  {
    return hasChildren();
  }

  /**
   * @see ReportObjective#getId()
   */
  public String getId()
  {
    return reportObjective.getId();
  }

  /**
   * @see ReportObjective#getTitle()
   */
  public String getTitle()
  {
    return reportObjective.getTitle();
  }

  public Report findById(final String id)
  {
    if (Objects.equals(this.reportObjective.getId(), id) == true) {
      return this;
    }
    if (hasChildren() == false) {
      return null;
    }
    for (final Report report : getChildren()) {
      if (Objects.equals(report.reportObjective.getId(), id) == true) {
        return report;
      }
    }
    for (final Report report : getChildren()) {
      final Report result = report.findById(id);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Creates and get the children if the ReportObjective has children. Iteriert über alle ChildReportObjectives und legt jeweils einen Report an
   * und selektiert gemäß Filter des ReportObjectives die Buchungssätze. Wenn Children nicht implizit erzeugt werden sollen, so sollte die
   * Funktion hasChildren zur Abfrage genutzt werden.
   * @see #select(List)
   */
  public List<Report> getChildren()
  {
    if (childReports == null && hasChildren() == true) {
      childReports = new ArrayList<Report>();
      for (final ReportObjective child : reportObjective.getChildReportObjectives()) {
        final Report report = new Report(child, this);
        report.select(this.buchungssaetze);
        childReports.add(report);
      }
      if (this.buchungssaetze != null && (reportObjective.isSuppressOther() == false || reportObjective.isSuppressDuplicates() == false)) {
        for (final BuchungssatzDO satz : this.buchungssaetze) {
          int n = 0;
          for (final Report child : getChildren()) {
            if (child.contains(satz) == true) {
              n++;
            }
          }
          if (reportObjective.isSuppressOther() == false && n == 0) {
            // Kommt bei keinem Childreport vor:
            if (other == null) {
              other = new ArrayList<BuchungssatzDO>();
            }
            other.add(satz);
          } else if (reportObjective.isSuppressDuplicates() == false && n > 1) {
            // Kommt bei mehreren Children vor:
            if (duplicates == null) {
              duplicates = new ArrayList<BuchungssatzDO>();
            }
            duplicates.add(satz);
          }
        }
      }
      if (reportObjective.isSuppressOther() == false && this.other != null) {
        final ReportObjective objective = new ReportObjective();
        final String other = ThreadLocalUserContext.getLocalizedString("fibu.reporting.other");
        objective.setId(this.getId() + " - " + other);
        objective.setTitle(this.getTitle() + " - " + other);
        final Report report = new Report(objective, this);
        report.setBuchungssaetze(this.other);
        childReports.add(report);
      }
      if (reportObjective.isSuppressDuplicates() == false && this.duplicates != null) {
        final ReportObjective objective = new ReportObjective();
        final String duplicates = ThreadLocalUserContext.getLocalizedString("fibu.reporting.duplicates");
        objective.setId(this.getId() + " - " + duplicates);
        objective.setTitle(this.getTitle() + " - " + duplicates);
        final Report report = new Report(objective, this);
        report.setBuchungssaetze(this.duplicates);
        childReports.add(report);
      }
    }
    return childReports;
  }

  public List<BuchungssatzDO> getBuchungssaetze()
  {
    return buchungssaetze;
  }

  /**
   * Bitte entweder diese Methode ODER select(...) benutzen.
   * @param buchungssaetze
   */
  public void setBuchungssaetze(final List<BuchungssatzDO> buchungssaetze)
  {
    this.buchungssaetze = buchungssaetze;
  }

  /**
   * Gibt die Liste aller sonstigen Buchungssätze zurück, d. h. Buchungssätze, die zwar in diesem Report vorkommen aber in keinem der
   * Childreports vorkommen.
   * @return Liste oder null, wenn keine Einträge vorhanden sind.
   */
  public List<BuchungssatzDO> getOther()
  {
    return other;
  }

  /**
   * Gibt die Liste aller doppelten Buchungssätze zurück, d. h. Buchungssätze, die in mehreren Childreports vorkommen.
   * @return Liste oder null, wenn keine Einträge vorhanden sind.
   */
  public List<BuchungssatzDO> getDuplicates()
  {
    return duplicates;
  }

  public String getZeitraum()
  {
    return KostFormatter.formatZeitraum(fromYear, fromMonth, toYear, toMonth);
  }

  /**
   * Diese initiale Liste der Buchungsliste wird sofort bezüglich Exclude- und Include-Filter selektiert und das Ergebnis gesetzt.
   * @param buchungssaetze vor Selektion.
   */
  public void select(final List<BuchungssatzDO> list)
  {
    final Predicate regExpPredicate = new Predicate() {
      @Override
      public boolean evaluate(final Object obj)
      {
        final BuchungssatzDO satz = (BuchungssatzDO) obj;
        final String kost1 = KostFormatter.format(satz.getKost1());
        final String kost2 = KostFormatter.format(satz.getKost2());

        // 1st of all the Blacklists
        if (match(reportObjective.getKost1ExcludeRegExpList(), kost1, false) == true) {
          return false;
        }
        if (match(reportObjective.getKost2ExcludeRegExpList(), kost2, false) == true) {
          return false;
        }
        // 2nd the whitelists
        final boolean kost1Match = match(reportObjective.getKost1IncludeRegExpList(), kost1, true);
        final boolean kost2Match = match(reportObjective.getKost2IncludeRegExpList(), kost2, true);
        return kost1Match == true && kost2Match == true;
      }
    };
    this.buchungssaetze = new ArrayList<BuchungssatzDO>();
    this.buchungssatzSet = new HashSet<BuchungssatzDO>();
    this.businessAssessment = null;
    this.businessAssessmentTable = null;
    this.childReports = null;
    this.duplicates = null;
    this.other = null;
    CollectionUtils.select(list, regExpPredicate, this.buchungssaetze);
    for (final BuchungssatzDO satz : this.buchungssaetze) {
      this.buchungssatzSet.add(satz);
    }
  }

  public boolean contains(final BuchungssatzDO satz)
  {
    if (buchungssatzSet == null) {
      return false;
    }
    return this.buchungssatzSet.contains(satz);
  }

  /**
   * In jedem regulärem Ausdruck werden alle Punkte gequoted und alle * durch ".*" ersetzt, bevor der Ausdruck durch
   * {@link Pattern#compile(String)} kompiliert wird.<br/>
   * Beispiele:
   * <ul>
   * <li>5.100.* -&gt; 5\.100\..*</li>
   * <li>*.10.* -&gt; .*\.10\..*</li>
   * </ul>
   * @param regExpList
   * @param kost
   * @param emptyListMatches
   * @return
   * @see String#matches(String)()
   * @see #modifyRegExp(String)
   */
  public static boolean match(final List<String> regExpList, final String kost, final boolean emptyListMatches)
  {
    if (CollectionUtils.isNotEmpty(regExpList) == true) {
      for (final String str : regExpList) {
        final String regExp = modifyRegExp(str);
        if (kost.matches(regExp) == true) {
          return true;
        }
      }
      return false;
    } else {
      // List is empty:
      return emptyListMatches;
    }
  }

  /**
   * Alle Punkte werden gequoted und alle * durch ".*" ersetzt. Ausnahme: Der String beginnt mit einem einfachen Hochkomma, dann werden
   * keine Ersetzungen durchgeführt, sondern lediglich das Hochkomma selbst entfernt.
   * @param regExp
   */
  public static String modifyRegExp(final String regExp)
  {
    if (regExp == null) {
      return null;
    }
    if (regExp.startsWith("'") == true) {
      return regExp.substring(1);
    }
    final String str = regExp.replace(".", "\\.").replace("*", ".*");
    return str;
  }
}
