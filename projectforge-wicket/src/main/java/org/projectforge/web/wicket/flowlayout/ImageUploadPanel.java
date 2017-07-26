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

package org.projectforge.web.wicket.flowlayout;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.validation.IValidator;
import org.projectforge.framework.utils.FileHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.dialog.ModalQuestionDialog;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.wicketstuff.html5.fileapi.FileFieldSizeCheckBehavior;
import org.wicketstuff.html5.fileapi.FileList;

/**
 * @author Florian Blumenstein
 */
public class ImageUploadPanel extends Panel implements ComponentWrapperPanel
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ImageUploadPanel.class);

  private static final long serialVersionUID = -4126462093466172226L;

  private FileUploadField fileUploadField;

  private IModel<byte[]> file;

  private ModalQuestionDialog deleteExistingFileDialog;

  private AjaxIconButtonPanel deleteFileButton;

  private NonCachingImage image;

  /**
   * @param id                 Component id
   * @param fs                 Optional FieldsetPanel for creation of filename with download link and upload button.
   * @param createFilenameLink If true (and fs is given) the filename is displayed with link for download.
   * @param
   */
  @SuppressWarnings("serial")
  public ImageUploadPanel(final String id, final FieldsetPanel fs, final AbstractEditForm<?, ?> form,
      final IModel<byte[]> file, final String maxImageSize)
  {
    super(id);

    // set max form size
    Bytes maxSize = Bytes.valueOf(maxImageSize);
    form.setMaxSize(maxSize);

    this.file = file;
    final WebMarkupContainer main = new WebMarkupContainer("main");
    add(main);
    main.setOutputMarkupId(true);

    // DELETE BUTTON
    if (this.file.getObject() != null) {
      if (fs != null) {
        deleteFileButton = new AjaxIconButtonPanel(fs.newChildId(), IconType.TRASH, fs.getString("delete"))
        {
          /**
           * @see org.projectforge.web.wicket.flowlayout.AjaxIconButtonPanel#onSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
           */
          @Override
          protected void onSubmit(final AjaxRequestTarget target)
          {
            deleteExistingFileDialog.open(target);
          }

          /**
           * @see org.apache.wicket.Component#isVisible()
           */
          @Override
          public boolean isButtonVisible()
          {
            return file.getObject() != null;
          }
        };
        deleteFileButton.getButton().setOutputMarkupPlaceholderTag(true);
        fs.add(deleteFileButton);
      }
    }

    // input element
    main.add(this.fileUploadField = new FileUploadField("input"));
    this.fileUploadField.add((IValidator<List<FileUpload>>) validatable -> {
      if (validatable == null) {
        return;
      }
      final List<FileUpload> list = validatable.getValue();
      if (list == null || list.size() == 0) {
        return;
      }
      if (list.size() > 1) {
        log.error("Multiple file uploads not yet supported. Uploading only first file.");
      }

      FileUpload fileUpload = list.get(0);
      String type = fileUpload.getContentType().split("/")[0];
      if (!type.equals("image")) {
        log.info("Uploaded file is no image. File type is " + type);
        return;
      }

      upload(fileUpload);
    });

    this.fileUploadField.add(new FileFieldSizeCheckBehavior()
    {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final FileList fileList)
      {
        // return form to remove errors
        target.add(form.getFeedbackPanel());
      }

      @Override
      protected void addErrorMsg(AjaxRequestTarget target, FileList fileList)
      {
        form.addError("common.uploadpanel.filetolarge", NumberHelper.formatBytes(maxSize.bytes()));
      }

      @Override
      protected void onError(final AjaxRequestTarget target, final FileList fileList)
      {
        // clear input field to avoid uploading the image
        ImageUploadPanel.this.fileUploadField.clearInput();

        // return form
        target.add(form);
      }
    });

    // image
    this.image = createImage();
    main.add(image);

    // delete dialog
    if (fs != null) {
      fs.add(this);
      final AbstractSecuredPage parentPage = (AbstractSecuredPage) getPage();
      deleteExistingFileDialog = new ModalQuestionDialog(parentPage.newModalDialogId(), new ResourceModel(
          "file.panel.deleteExistingFile.heading"), new ResourceModel("file.panel.deleteExistingFile.question"))
      {
        /**
         * @see org.projectforge.web.dialog.ModalQuestionDialog#onCloseButtonSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
         */
        @Override
        protected boolean onCloseButtonSubmit(final AjaxRequestTarget target)
        {
          super.onCloseButtonSubmit(target);
          if (isConfirmed() == true) {
            file.setObject(null);
            main.setVisible(true).setOutputMarkupPlaceholderTag(true);
            target.add(deleteFileButton.getButton(), main);
            target.add(image, main);
          }
          return true;
        }
      };
      parentPage.add(deleteExistingFileDialog);
      deleteExistingFileDialog.init();
    }
  }

  private NonCachingImage createImage()
  {
    NonCachingImage img = new NonCachingImage("image", new IModel<DynamicImageResource>()
    {
      @Override
      public DynamicImageResource getObject()
      {
        final DynamicImageResource dir = new DynamicImageResource()
        {
          @Override
          protected byte[] getImageData(final Attributes attributes)
          {
            byte[] result = file.getObject();
            if (result == null || result.length < 1) {
              try {
                result = IOUtils.toByteArray(getClass().getClassLoader().getResource("images/noImage.png").openStream());
              } catch (final IOException ex) {
                log.error("Exception encountered " + ex, ex);
              }
            }
            return result;
          }
        };
        dir.setFormat("image/png");
        return dir;
      }
    });
    img.setOutputMarkupId(true);
    img.add(new AttributeModifier("height", Integer.toString(200)));
    return img;
  }

  /**
   * Called by validator.
   */
  protected void upload(final FileUpload fileUpload)
  {
    if (fileUpload != null) {
      final String clientFileName = FileHelper.createSafeFilename(fileUpload.getClientFileName(), 255);
      log.info("Upload file '" + clientFileName + "'.");
      final byte[] bytes = fileUpload.getBytes();
      file.setObject(bytes);
    }
  }

  /**
   * Calls {@link Form#setMultiPart(boolean)} with value true.
   *
   * @see org.apache.wicket.Component#onBeforeRender()
   */
  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    if (this.fileUploadField.getForm() != null) {
      this.fileUploadField.getForm().setMultiPart(true);
    }
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    fileUploadField.setOutputMarkupId(true);
    return fileUploadField.getMarkupId();
  }

  /**
   * @return the field
   */
  public FileUploadField getFileUploadField()
  {
    return fileUploadField;
  }

  public FileUpload getFileUpload()
  {
    return fileUploadField.getFileUpload();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent<?> getFormComponent()
  {
    return fileUploadField;
  }
}
