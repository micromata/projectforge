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

package org.projectforge.web.book;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.projectforge.Const;
import org.projectforge.business.book.BookDO;
import org.projectforge.business.book.BookDao;
import org.projectforge.business.book.BookStatus;
import org.projectforge.business.book.BookType;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.excel.ExcelDateFormats;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;

public class BookEditForm extends AbstractEditForm<BookDO, BookEditPage>
{
  private static final long serialVersionUID = 3881031215413525517L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BookEditForm.class);

  @SpringBean
  private BookDao bookDao;

  // Components for form validation.
  @SuppressWarnings("unchecked")
  private final TextField<String>[] dependentFormComponents = new TextField[5];

  public BookEditForm(final BookEditPage parentPage, final BookDO data)
  {
    super(parentPage, data);
    if (isNew() == true) {
      data.setStatus(BookStatus.PRESENT);
      data.setType(BookType.BOOK);
    }
    if (getData().getTaskId() == null) {
      bookDao.setTask(getData(), bookDao.getDefaultTaskId());
    }
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    add(new IFormValidator()
    {
      @Override
      public FormComponent<?>[] getDependentFormComponents()
      {
        return dependentFormComponents;
      }

      @Override
      public void validate(final Form<?> form)
      {
        final TextField<String> authorsField = dependentFormComponents[0];
        final TextField<String> yearOfPublishingField = dependentFormComponents[1];
        final TextField<String> signatureField = dependentFormComponents[2];
        final TextField<String> publisherField = dependentFormComponents[3];
        final TextField<String> editorField = dependentFormComponents[4];
        if (StringUtils.isBlank(authorsField.getConvertedInput()) == true
            && StringUtils.isBlank(publisherField.getConvertedInput()) == true
            && StringUtils.isBlank(editorField.getConvertedInput()) == true
            && StringUtils.isBlank(signatureField.getConvertedInput()) == true
            && StringUtils.isBlank(yearOfPublishingField.getConvertedInput()) == true) {
          error(getString("book.error.toFewFields"));
        }
        final int year = Integer.parseInt(yearOfPublishingField.getConvertedInput());
        if (year < Const.MINYEAR || year > Const.MAXYEAR) {
          form.error(I18nHelper.getLocalizedMessage("error.yearOutOfRange", Const.MINYEAR, Const.MAXYEAR));
        }
      }
    });

    gridBuilder.newGridPanel();
    {
      // Title
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("book.title"));
      final RequiredMaxLengthTextField title = new RequiredMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "title"));
      title.add(WicketUtils.setFocus());
      fs.add(title);
    }
    {
      // Authors
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("book.authors"));
      final MaxLengthTextField authors = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "authors"));
      fs.add(dependentFormComponents[0] = authors);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // DropDownChoice bookType
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("book.type"));
      final LabelValueChoiceRenderer<BookType> bookTypeChoiceRenderer = new LabelValueChoiceRenderer<BookType>(this,
          BookType.values());
      final DropDownChoice<BookType> bookTypeChoice = new DropDownChoice<BookType>(fs.getDropDownChoiceId(),
          new PropertyModel<BookType>(data, "type"), bookTypeChoiceRenderer.getValues(), bookTypeChoiceRenderer);
      bookTypeChoice.setNullValid(false).setRequired(true);
      fs.add(bookTypeChoice);
    }
    {
      // Year of publishing
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("book.yearOfPublishing"));
      final MaxLengthTextField yearOfPublishing = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data,
              "yearOfPublishing"));
      fs.add(dependentFormComponents[1] = yearOfPublishing);
    }
    {
      // DropDownChoice bookStatus
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("status"));
      final LabelValueChoiceRenderer<BookStatus> bookStatusChoiceRenderer = new LabelValueChoiceRenderer<BookStatus>(
          this,
          BookStatus.values());
      final DropDownChoice<BookStatus> bookStatusChoice = new DropDownChoice<BookStatus>(fs.getDropDownChoiceId(),
          new PropertyModel<BookStatus>(data, "status"), bookStatusChoiceRenderer.getValues(),
          bookStatusChoiceRenderer);
      bookStatusChoice.setNullValid(false).setRequired(true);
      fs.add(bookStatusChoice);
    }
    {
      // Signature
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("book.signature"));
      final MaxLengthTextField signature = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "signature"));
      signature.add(new AbstractValidator<String>()
      {
        private static final long serialVersionUID = 6488923290863235755L;

        @Override
        protected void onValidate(final IValidatable<String> validatable)
        {
          data.setSignature(validatable.getValue());
          if (bookDao.doesSignatureAlreadyExist(data) == true) {
            validatable.error(new ValidationError().addMessageKey("book.error.signatureAlreadyExists"));
          }
        }
      });
      fs.add(dependentFormComponents[2] = signature);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // ISBN
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("book.isbn"));
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "isbn")));
    }
    {
      // Keywords
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("book.keywords"));
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "keywords")));
    }
    {
      // Publisher
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("book.publisher"));
      final MaxLengthTextField publisher = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "publisher"));
      fs.add(dependentFormComponents[3] = publisher);
    }
    {
      // Editor
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("book.editor"));
      final MaxLengthTextField editor = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "editor"));
      fs.add(dependentFormComponents[4] = editor);
    }
    gridBuilder.newGridPanel();
    {
      // Abstract
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("book.abstract"));
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "abstractText")));
    }
    {
      // Comment
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("comment"));
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "comment")));
    }
    if (isNew() == false) {
      {
        // Lend out
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("book.lending")).suppressLabelForWarning();
        fs.add(new DivTextPanel(fs.newChildId(), new Model<String>()
        {
          /**
           * @see org.apache.wicket.model.Model#getObject()
           */
          @Override
          public String getObject()
          {
            if (data.getLendOutBy() == null) {
              return "";
            }
            final StringBuffer buf = new StringBuffer();
            // Show full user name:
            buf.append(data.getLendOutBy().getFullname());
            if (data.getLendOutDate() != null) {
              buf.append(", ");
              // Show lend out date:
              buf.append(DateTimeFormatter.instance().getFormattedDate(data.getLendOutDate()));
            }
            return buf.toString();
          }
        })
        {
          /**
           * @see org.apache.wicket.Component#isVisible()
           */
          @Override
          public boolean isVisible()
          {
            return data.getLendOutBy() != null;
          }
        });
        fs.add(
            new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID, new Model<String>("lendout"))
            {
              @Override
              public final void onSubmit()
              {
                parentPage.lendOut();
              }
            }, getString("book.lendOut"), SingleButtonPanel.NORMAL));
        fs.add(new SingleButtonPanel(fs.newChildId(),
            new Button(SingleButtonPanel.WICKET_ID, new Model<String>("returnBook"))
            {
              @Override
              public final void onSubmit()
              {
                parentPage.returnBook();
              }
            }, getString("book.returnBook"), SingleButtonPanel.DANGER)
        {
          /**
           * @see org.apache.wicket.Component#isVisible()
           */
          @Override
          public boolean isVisible()
          {
            return data.getLendOutBy() != null;
          }
        });
      }
      {
        // Lend out comment
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("book.lendOutNote"));
        fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "lendOutComment")));
      }
    }

  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
