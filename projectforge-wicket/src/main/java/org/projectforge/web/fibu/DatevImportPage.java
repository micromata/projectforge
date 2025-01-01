/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.merlin.excel.importer.ImportedElement;
import de.micromata.merlin.excel.importer.ImportedSheet;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.fibu.datev.DatevImportService;
import org.projectforge.business.fibu.kost.AccountingConfig;
import org.projectforge.business.fibu.kost.BuchungssatzDO;
import org.projectforge.business.fibu.kost.BusinessAssessment;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.common.logging.LogEventLoggerNameMatcher;
import org.projectforge.common.logging.LogSubscription;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.rest.admin.LogViewerPageRest;
import org.projectforge.rest.core.PagesResolver;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.core.importstorage.AbstractImportPage;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DatevImportPage extends AbstractImportPage<DatevImportForm> {
  private static final long serialVersionUID = 3158445617725488919L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatevImportPage.class);

  public DatevImportPage(final PageParameters parameters) {
    super(parameters);
    final String username = ThreadLocalUserContext.getLoggedInUser().getUsername();
    final LogSubscription logSubscription = LogSubscription.ensureSubscription("Datev-Import", username,
        (title, user) -> new LogSubscription(title, user, new LogEventLoggerNameMatcher("org.projectforge.business.fibu.datev", "de.micromata.merlin")));

    form = new DatevImportForm(this);
    body.add(form);
    form.init();
    final ExternalLink logViewerLink = new ExternalLink(ContentMenuEntryPanel.LINK_ID, PagesResolver.getDynamicPageUrl(LogViewerPageRest.class, null, logSubscription.getId(), true));
    addContentMenuEntry(new ContentMenuEntryPanel(getNewContentMenuChildId(), logViewerLink, getString("system.admin.logViewer.title")));
  }

  protected void importAccountList() {
    checkAccess();
    final FileUpload fileUpload = form.fileUploadField.getFileUpload();
    if (fileUpload != null) {
      doImportWithExcelExceptionHandling(() -> {
        final InputStream is = fileUpload.getInputStream();
        final String clientFileName = fileUpload.getClientFileName();
        setStorage(WicketSupport.get(DatevImportService.class).importKontenplan(is, clientFileName));
        return null;
      });
    }
  }

  protected void importAccountRecords() {
    checkAccess();
    final FileUpload fileUpload = form.fileUploadField.getFileUpload();
    if (fileUpload != null) {
      doImportWithExcelExceptionHandling(() -> {
        final InputStream is = fileUpload.getInputStream();
        final String clientFileName = fileUpload.getClientFileName();
        setStorage(WicketSupport.get(DatevImportService.class).importBuchungsdaten(is, clientFileName));
        return null;
      });
    }
  }

  @Override
  protected ImportedSheet<?> reconcile(final String sheetName) {
    checkAccess();
    final ImportedSheet<?> sheet = super.reconcile(sheetName);
    WicketSupport.get(DatevImportService.class).reconcile(getStorage(), sheetName);
    return sheet;
  }

  @Override
  protected ImportedSheet<?> commit(final String sheetName) {
    checkAccess();
    final ImportedSheet<?> sheet = super.commit(sheetName);
    WicketSupport.get(DatevImportService.class).commit(getStorage(), sheetName);
    return sheet;
  }

  @Override
  protected void selectAll(final String sheetName) {
    checkAccess();
    super.selectAll(sheetName);
  }

  @Override
  protected void select(final String sheetName, final int number) {
    checkAccess();
    super.select(sheetName, number);
  }

  @Override
  protected void deselectAll(final String sheetName) {
    checkAccess();
    super.deselectAll(sheetName);
  }

  protected void showBusinessAssessment(final String sheetName) {
    final ImportedSheet<?> sheet = getStorage().getNamedSheet(sheetName);
    Validate.notNull(sheet);
    final List<BuchungssatzDO> list = new ArrayList<BuchungssatzDO>();
    for (final ImportedElement<?> element : sheet.getElements()) {
      final BuchungssatzDO satz = (BuchungssatzDO) element.getValue();
      list.add(satz);
    }
    final BusinessAssessment businessAssessment = new BusinessAssessment(
        AccountingConfig.getInstance().getBusinessAssessmentConfig(),
        (Integer) sheet.getProperty("year"), (Integer) sheet.getProperty("month"));
    form.setBusinessAssessment(businessAssessment);
    businessAssessment.setAccountRecords(list);
  }

  private void checkAccess() {
    getAccessChecker().checkLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.TRUE);
    getAccessChecker().checkRestrictedOrDemoUser();
  }

  @Override
  protected String getTitle() {
    return getString("fibu.datev.import");
  }
}
