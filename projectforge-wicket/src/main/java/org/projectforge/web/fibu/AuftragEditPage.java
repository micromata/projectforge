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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.SystemStatus;
import org.projectforge.business.fibu.*;
import org.projectforge.business.fibu.kost.ProjektCache;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.json.JsonUtils;
import org.projectforge.framework.persistence.api.EntityCopyStatus;
import org.projectforge.framework.time.PFDay;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.core.NavTopPanel;
import org.projectforge.web.wicket.*;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@EditPage(defaultReturnPage = AuftragListPage.class)
public class AuftragEditPage extends AbstractEditPage<AuftragDO, AuftragEditForm, AuftragDao>
        implements ISelectCallerPage {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuftragEditPage.class);

    private static final long serialVersionUID = -8192471994161712577L;

    private HtmlPreviewModalDialog htmlPreviewModalDialog;

    public AuftragEditPage(final PageParameters parameters) {
        super(parameters, "fibu.auftrag");
        init();
        if (isNew() && getData().getContactPerson() == null) {
            WicketSupport.get(AuftragDao.class).setContactPerson(getData(), getUser().getId());
        }
        Long orderId = getData().getId();
        if (orderId != null ) {
            htmlPreviewModalDialog = new HtmlPreviewModalDialog(this.newModalDialogId());
            htmlPreviewModalDialog.setOutputMarkupId(true);
            add(htmlPreviewModalDialog);
            htmlPreviewModalDialog.init();
            AjaxSubmitLink newWindowLink = new AjaxSubmitLink(ContentMenuEntryPanel.LINK_ID, this.form) {
                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    htmlPreviewModalDialog.open(target);
                    // Redraw the content:
                    htmlPreviewModalDialog.redraw(getData()).addContent(target);
                }

                @Override
                protected void onError(AjaxRequestTarget target) {
                }
            };
            ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(), newWindowLink, getString("fibu.auftrag.exportAnalysis"));
            addContentMenuEntry(menu);
            if (SystemStatus.isDevelopmentMode()) {
                menu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
                        new Link<Void>(ContentMenuEntryPanel.LINK_ID) {
                            @Override
                            public void onClick() {
                                var list = WicketSupport.get(ForecastOrderAnalysis.class).exportOrderAnalysis(getData().getId());
                                if (list != null) {
                                    String filename = "orderAnalysis-" + getData().getNummer() + ".json";
                                    DownloadUtils.setDownloadTarget(JsonUtils.toJson(list).getBytes(StandardCharsets.UTF_8), filename);
                                }
                            }
                        }, "Export as json (dev)");
                addContentMenuEntry(menu);
            }
        }
    }

    @Override
    protected AuftragDao getBaseDao() {
        return WicketSupport.get(AuftragDao.class);
    }

    @Override
    protected AuftragEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final AuftragDO data) {
        return new AuftragEditForm(this, data);
    }

    /**
     * @see org.projectforge.web.fibu.ISelectCallerPage#select(String, Object)
     */
    @Override
    public void select(final String property, final Object selectedValue) {
        if ("projektId".equals(property)) {
            WicketSupport.get(AuftragDao.class).setProjekt(getData(), (Long) selectedValue);
            form.projektSelectPanel.getTextField().modelChanged();
            final ProjektDO projekt = WicketSupport.get(ProjektCache.class).getProjektIfNotInitialized(getData().getProjekt());
            if (projekt != null) {
                form.setKundePmHobmAndSmIfEmpty(projekt, null);
            }
        } else if ("kundeId".equals(property)) {
            WicketSupport.get(AuftragDao.class).setKunde(getData(), (Long) selectedValue);
            form.kundeSelectPanel.getTextField().modelChanged();
        } else if ("contactPersonId".equals(property)) {
            WicketSupport.get(AuftragDao.class).setContactPerson(getData(), (Long) selectedValue);
            setSendEMailNotification();
        } else if (property.startsWith("taskId:")) {
            final Short number = NumberHelper.parseShort(property.substring(property.indexOf(':') + 1));
            final AuftragsPositionDO pos = getData().getPosition(number);
            WicketSupport.get(AuftragDao.class).setTask(pos, (Long) selectedValue);
        } else {
            log.error("Property '" + property + "' not supported for selection.");
        }
    }

    private void setSendEMailNotification() {
        if (getAccessChecker().userEqualsToContextUser(getData().getContactPerson()))
            form.setSendEMailNotification(false);
        else
            form.setSendEMailNotification(true);
    }

    /**
     * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
     */
    @Override
    public void unselect(final String property) {
        if ("projektId".equals(property)) {
            getData().setProjekt(null);
            form.projektSelectPanel.getTextField().modelChanged();
        } else if ("kundeId".equals(property)) {
            getData().setKunde(null);
            form.kundeSelectPanel.getTextField().modelChanged();
        } else if ("contactPersonId".equals(property)) {
            getData().setContactPerson(null);
            setSendEMailNotification();
        } else if (property.startsWith("taskId:")) {
            final Short number = NumberHelper.parseShort(property.substring(property.indexOf(':') + 1));
            final AuftragsPositionDO pos = getData().getPosition(number);
            pos.setTask(null);
        } else {
            log.error("Property '" + property + "' not supported for selection.");
        }
    }

    /**
     * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
     */
    @Override
    public void cancelSelection(final String property) {
        // Do nothing.
    }

    @Override
    public AbstractSecuredBasePage onSaveOrUpdate() {
        if (getData().getNummer() == null) {
            getData().setNummer(WicketSupport.get(AuftragDao.class).getNextNumber(getData()));
        }
        if (getData().getKunde() != null) {
            getData().setKundeText(null);
        }
        return null;
    }

    @Override
    protected void update() {
        AuftragDO auftrag = getData();
        AuftragDO dbObj = WicketSupport.get(AuftragDao.class).find(auftrag.getId(), false);
        if (dbObj != null) {
            // Update attachments values (if modified in the mean time. They can't be modified on this Wicket page):
            auftrag.setAttachmentsCounter(dbObj.getAttachmentsCounter());
            auftrag.setAttachmentsNames(dbObj.getAttachmentsNames());
            auftrag.setAttachmentsIds(dbObj.getAttachmentsIds());
            auftrag.setAttachmentsSize(dbObj.getAttachmentsSize());
            auftrag.setAttachmentsLastUserAction(dbObj.getAttachmentsLastUserAction());
        }
        super.update();
    }

    @Override
    protected void onPreEdit() {
        final AuftragDO auftrag = getData();

        if (auftrag.getId() == null) {
            if (auftrag.getAngebotsDatum() == null) {
                final LocalDate today = LocalDate.now();
                auftrag.setAngebotsDatum(today);
                auftrag.setErfassungsDatum(today);
                auftrag.setEntscheidungsDatum(today);
            }
            if (auftrag.getContactPerson() == null && getAccessChecker().isLoggedInUserMemberOfGroup(ProjectForgeGroup.PROJECT_MANAGER)) {
                WicketSupport.get(AuftragDao.class).setContactPerson(auftrag, getUser().getId());
                form.setSendEMailNotification(false);
            }
        } else if (auftrag.getErfassungsDatum() == null) {
            if (auftrag.getCreated() != null) {
                auftrag.setErfassungsDatum(PFDay.from(auftrag.getCreated()).getLocalDate());
            } else if (auftrag.getAngebotsDatum() != null) {
                auftrag.setErfassungsDatum(auftrag.getAngebotsDatum());
            } else {
                auftrag.setErfassungsDatum(LocalDate.now());
            }
        } else {
            setSendEMailNotification();
        }

        auftrag.recalculate();
    }

    @Override
    public AbstractSecuredBasePage afterSave() {
        if (!form.isSendEMailNotification()) {
            return null;
        }
        sendNotificationIfRequired(OperationType.INSERT);
        return null;
    }

    @Override
    public AbstractSecuredBasePage afterUpdate(final EntityCopyStatus modified) {
        if (!form.isSendEMailNotification()) {
            return null;
        }
        if (modified == EntityCopyStatus.MAJOR) {
            sendNotificationIfRequired(OperationType.UPDATE);
        }
        return null;
    }

    private void sendNotificationIfRequired(final OperationType operationType) {
        final String url = getPageAsLink(WicketUtils.getEditPageParameters(getData().getId()));
        WicketSupport.get(AuftragDao.class).sendNotificationIfRequired(getData(), operationType, url);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
