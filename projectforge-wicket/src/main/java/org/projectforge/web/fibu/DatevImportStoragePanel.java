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

package org.projectforge.web.fibu;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.datev.DatevImportDao;
import org.projectforge.business.fibu.kost.BuchungssatzDO;
import org.projectforge.business.fibu.kost.BusinessAssessment;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.framework.persistence.utils.ImportedElement;
import org.projectforge.framework.persistence.utils.ImportedSheet;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.core.importstorage.AbstractImportStoragePanel;
import org.projectforge.web.core.importstorage.ImportFilter;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class DatevImportStoragePanel extends AbstractImportStoragePanel<DatevImportPage>
{
  private static final long serialVersionUID = -5732520730823126042L;

  private Label businessAssessmentLabel;

  protected BusinessAssessment businessAssessment;

  /**
   * @param id
   */
  public DatevImportStoragePanel(final String id, final DatevImportPage parentPage, final ImportFilter filter)
  {
    super(id, parentPage, filter);
  }

  @Override
  public void refresh()
  {
    super.refresh();
    if (businessAssessmentLabel != null) {
      remove(businessAssessmentLabel);
    }
  }

  @SuppressWarnings("serial")
  @Override
  protected void appendSheetActionLinks(final ImportedSheet< ? > sheet, final RepeatingView actionLinkRepeater)
  {
    if (getStorageType() == DatevImportDao.Type.BUCHUNGSSAETZE) {
      addActionLink(actionLinkRepeater, new SubmitLink("actionLink") {
        @Override
        public void onSubmit()
        {
          parentPage.showBusinessAssessment(sheet.getName());
        }
      }, "show business assessment");
    }
  }

  /**
   * @see org.projectforge.web.core.importstorage.AbstractImportStoragePanel#addHeadColumns(org.apache.wicket.markup.repeater.RepeatingView)
   */
  @Override
  protected void addHeadColumns(final RepeatingView headColRepeater)
  {
    if (getStorageType() == DatevImportDao.Type.KONTENPLAN) {
      headColRepeater.add(new Label(headColRepeater.newChildId(), getString("fibu.konto.nummer")));
      headColRepeater.add(new Label(headColRepeater.newChildId(), getString("fibu.konto.bezeichnung")));
    } else {
      headColRepeater.add(new Label(headColRepeater.newChildId(), getString("fibu.buchungssatz.satznr")));
      headColRepeater.add(new Label(headColRepeater.newChildId(), getString("date")));
      headColRepeater.add(new Label(headColRepeater.newChildId(), getString("fibu.common.betrag")));
      headColRepeater.add(new Label(headColRepeater.newChildId(), getString("fibu.buchungssatz.text")));
      headColRepeater.add(new Label(headColRepeater.newChildId(), getString("fibu.buchungssatz.konto")));
      headColRepeater.add(new Label(headColRepeater.newChildId(), getString("fibu.buchungssatz.gegenKonto")));
      headColRepeater.add(new Label(headColRepeater.newChildId(), getString("fibu.kost1")));
      headColRepeater.add(new Label(headColRepeater.newChildId(), getString("fibu.kost2")));
    }
  }

  /**
   * @see org.projectforge.web.core.importstorage.AbstractImportStoragePanel#addColumns(org.apache.wicket.markup.repeater.RepeatingView,
   *      org.projectforge.framework.persistence.utils.ImportedElement)
   */
  @Override
  protected void addColumns(final RepeatingView cellRepeater, final ImportedElement< ? > element, final String style)
  {
    if (getStorageType() == DatevImportDao.Type.KONTENPLAN) {
      final KontoDO konto = (KontoDO) element.getValue();
      addCell(cellRepeater, konto.getNummer(), style + " white-space: nowrap; text-align: right;");
      addCell(cellRepeater, konto.getBezeichnung(), style);
    } else {
      final BuchungssatzDO satz = (BuchungssatzDO) element.getValue();
      addCell(cellRepeater, satz.getSatznr(), style + " white-space: nowrap; text-align: right;");
      addCell(cellRepeater, DateTimeFormatter.instance().getFormattedDate(satz.getDatum()), style + " white-space: nowrap;");
      addCell(cellRepeater, CurrencyFormatter.format(satz.getBetrag()), style + " white-space: nowrap; text-align: right;");
      addCell(cellRepeater, satz.getText(), style);
      addCell(cellRepeater, satz.getKonto() != null ? satz.getKonto().getNummer() : null, style);
      addCell(cellRepeater, satz.getGegenKonto() != null ? satz.getGegenKonto().getNummer() : null, style);
      final Kost1DO kost1 = satz.getKost1();
      Component comp = addCell(cellRepeater, kost1 != null ? kost1.getShortDisplayName() : null, style);
      if (kost1 != null) {
        WicketUtils.addTooltip(comp, KostFormatter.formatToolTip(kost1));
      }
      final Kost2DO kost2 = satz.getKost2();
      comp = addCell(cellRepeater, kost2 != null ? kost2.getShortDisplayName() : null, style);
      if (kost2 != null) {
        WicketUtils.addTooltip(comp, KostFormatter.formatToolTip(kost2));
      }
    }
  }

  private DatevImportDao.Type getStorageType()
  {
    if (storage == null) {
      return null;
    } else {
      return (DatevImportDao.Type) storage.getId();
    }
  }
}
