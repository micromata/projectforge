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

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.fibu.kost.Kost2ArtDO;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.components.RequiredMinMaxNumberField;
import org.projectforge.web.wicket.converter.LongConverter;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;
import org.slf4j.Logger;

import java.math.BigDecimal;

public class Kost2ArtEditForm extends AbstractEditForm<Kost2ArtDO, Kost2ArtEditPage> {
    private static final long serialVersionUID = 1207258100682337083L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Kost2ArtEditForm.class);

    public Kost2ArtEditForm(final Kost2ArtEditPage parentPage, final Kost2ArtDO data) {
        super(parentPage, data);
    }

    @Override
    @SuppressWarnings("serial")
    protected void init() {
        super.init();
        gridBuilder.newGridPanel();
        {
            // Number
            final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kost2art.nummer"));
            final TextField<Long> nummerField = new RequiredMinMaxNumberField<Long>(InputPanel.WICKET_ID, new PropertyModel<Long>(data,
                    "id"), 0L, 99L) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                @Override
                public IConverter getConverter(final Class type) {
                    return new LongConverter(2);
                }
            };
            if (isNew() == false) {
                nummerField.setEnabled(false);
            }
            fs.add(nummerField);
        }
        {
            // Invoiced
            gridBuilder.newFieldset(getString("fibu.fakturiert")).addCheckBox(new PropertyModel<Boolean>(data, "fakturiert"), null);
        }
        {
            // Invoiced
            gridBuilder.newFieldset(getString("fibu.kost2art.projektStandard")).addCheckBox(new PropertyModel<Boolean>(data, "projektStandard"),
                    null);
        }
        {
            // Name
            final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kost2art.name"));
            fs.add(new RequiredMaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "name")));
        }
        {
            // Work frqction
            final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kost2art.workFraction"));
            fs.add(new MinMaxNumberField<BigDecimal>(InputPanel.WICKET_ID, new PropertyModel<BigDecimal>(data, "workFraction"), BigDecimal.ZERO,
                    BigDecimal.ONE));
        }
        {
            // Description
            final FieldsetPanel fs = gridBuilder.newFieldset(getString("description"));
            fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "description")), true);
        }
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
