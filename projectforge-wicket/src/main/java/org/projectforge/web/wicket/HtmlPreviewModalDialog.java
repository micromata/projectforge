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

package org.projectforge.web.wicket;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.ForecastOrderAnalysis;
import org.projectforge.business.fibu.OrderInfo;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.dialog.ModalDialog;

public class HtmlPreviewModalDialog extends ModalDialog {
    private String htmlContent = "";

    public HtmlPreviewModalDialog(String id) {
        super(id);
        setBigWindow();
    }

    @Override
    public void init() {
        setTitle(getString("fibu.auftrag.forecast"));
        init(new Form<String>(getFormId()));
        gridBuilder.newFormHeading(""); // Otherwise it's empty and an IllegalArgumentException is thrown.
    }

    public HtmlPreviewModalDialog redraw(AuftragDO order) {
        OrderInfo orderInfo = order.getInfo();
        orderInfo.calculateAll(order);
        clearContent();
        htmlContent = WicketSupport.get(ForecastOrderAnalysis.class).htmlExport(orderInfo);
        gridBuilder.newRepeatingView().add(new Label("htmlContent", Model.of(htmlContent)).setEscapeModelStrings(false));
        return this;
    }
}
