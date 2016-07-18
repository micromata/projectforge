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

package org.projectforge.web.imagecropper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.WicketUtils;

public class ImageCropperPage extends AbstractSecuredPage
{
  private static final long serialVersionUID = -3868048775620052627L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ImageCropperPage.class);

  public static final String PARAM_ENABLE_WHITEBOARD_FILTER = "enableWhiteboardFilter";

  public static final String PARAM_SHOW_UPLOAD_BUTTON = "showUploadButton";

  public static final String PARAM_LANGUAGE = "language";

  public static final String PARAM_RATIOLIST = "ratioList";

  public static final String PARAM_DEFAULT_RATIO = "defaultRatio";

  public static final String PARAM_FILE_FORMAT = "fileFormat";

  private String ratioList = "1:1,1:2,2:1,1:3,2:3,3:1,3:2,1:4,3:4,4:1,4:3,1:5,2:5,3:5,4:5,5:1,5:2,5:3,5:4";

  private String defaultRatio = "2:3";

  private boolean showUploadButton;

  private boolean enableWhiteBoardFilter;

  private String defaultLanguage;

  private String fileFormat = "";

  @SpringBean
  private ConfigurationService configService;

  /**
   * See list of constants PARAM_* for supported parameters.
   * 
   * @param parameters
   */
  public ImageCropperPage(final PageParameters parameters)
  {
    super(parameters);
    if (WicketUtils.contains(parameters, PARAM_SHOW_UPLOAD_BUTTON) == true) {
      setEnableWhiteBoardFilter(WicketUtils.getAsBoolean(parameters, PARAM_SHOW_UPLOAD_BUTTON));
    }
    if (WicketUtils.contains(parameters, PARAM_ENABLE_WHITEBOARD_FILTER) == true) {
      setEnableWhiteBoardFilter(WicketUtils.getAsBoolean(parameters, PARAM_ENABLE_WHITEBOARD_FILTER));
    }
    if (WicketUtils.contains(parameters, PARAM_LANGUAGE) == true) {
      setDefaultLanguage(WicketUtils.getAsString(parameters, PARAM_LANGUAGE));
    }
    if (WicketUtils.contains(parameters, PARAM_RATIOLIST) == true) {
      setRatioList(WicketUtils.getAsString(parameters, PARAM_RATIOLIST));
    }
    if (WicketUtils.contains(parameters, PARAM_DEFAULT_RATIO) == true) {
      setDefaultRatio(WicketUtils.getAsString(parameters, PARAM_DEFAULT_RATIO));
    }
    if (WicketUtils.contains(parameters, PARAM_FILE_FORMAT) == true) {
      setFileFormat(WicketUtils.getAsString(parameters, PARAM_FILE_FORMAT));
    }
    final ServletWebRequest req = (ServletWebRequest) this.getRequest();
    final HttpServletRequest hreq = req.getContainerRequest();
    String domain;
    if (StringUtils.isNotBlank(configService.getDomain()) == true) {
      domain = configService.getDomain();
    } else {
      domain = hreq.getScheme() + "://" + hreq.getLocalName() + ":" + hreq.getLocalPort();
    }
    final String url = domain + hreq.getContextPath() + "/secure/";
    final StringBuffer buf = new StringBuffer();
    appendVar(buf, "serverURL", url); // TODO: Wird wohl nicht mehr gebraucht.
    appendVar(buf, "uploadImageFileTemporaryServlet", url + "UploadImageFileTemporary");
    appendVar(buf, "uploadImageFileTemporaryServletParams", "filedirectory=tempimages;filename=image");
    appendVar(buf, "downloadImageFileServlet", url + "DownloadImageFile");
    appendVar(buf, "downloadImageFileServletParams", "filedirectory=tempimages;filename=image");
    appendVar(buf, "uploadImageFileServlet", url + "UploadImageFile");
    appendVar(buf, "uploadImageFileServletParams", "filedirectory=images;filename=image;croppedname=cropped");
    appendVar(buf, "upAndDownloadImageFileAsByteArrayServlet", url + "UpAndDownloadImageFileAsByteArray");
    appendVar(buf, "upAndDownloadImageFileAsByteArrayServletParams", "filename=image;croppedname=cropped");
    final HttpSession httpSession = hreq.getSession();
    appendVar(buf, "sessionid", httpSession.getId());
    appendVar(buf, "ratioList", ratioList);
    appendVar(buf, "defaultRatio", defaultRatio);
    appendVar(buf, "isUploadBtn", showUploadButton);
    appendVar(buf, "whiteBoardFilter", enableWhiteBoardFilter);
    appendVar(buf, "language", getDefaultLanguage());
    appendVar(buf, "fileFormat", fileFormat);
    appendVar(buf, "flashFile", WicketUtils.getAbsoluteUrl("/imagecropper/MicromataImageCropper"));
    add(new Label("javaScriptVars", buf.toString()).setEscapeModelStrings(false));
  }

  @Override
  public void renderHead(final IHeaderResponse response)
  {
    super.renderHead(response);
    response.render(CssReferenceHeaderItem.forUrl("imagecropper/history/history.css"));
    response.render(JavaScriptReferenceHeaderItem.forUrl("imagecropper/history/history.js"));
    response.render(JavaScriptReferenceHeaderItem.forUrl("imagecropper/AC_OETags.js"));
  }

  /**
   * Valid Ratio Examples: "1:4, 4:1, 1:2, 2:1, 1:3, 3:1, 2:3, 3:2" etc.
   */
  public String getRatioList()
  {
    return ratioList;
  }

  public void setRatioList(final String ratioList)
  {
    this.ratioList = ratioList;
  }

  /**
   * Wird Variable leer gelassen, kann die Ratio frei gewählt werden Wird Variable mit gültigem Wert befüllt, wird die
   * Ratio auf den Konfigurierten Wert gesetzt
   */
  public String getDefaultRatio()
  {
    return defaultRatio;
  }

  public void setDefaultRatio(final String defaultRatio)
  {
    this.defaultRatio = defaultRatio;
  }

  /**
   * If true then the upload button in ImageCropper flash app will be shown.
   */
  public boolean isShowUploadButton()
  {
    return showUploadButton;
  }

  public void setShowUploadButton(final boolean showUploadButton)
  {
    this.showUploadButton = showUploadButton;
  }

  /**
   * Auf true gesetzt kann WhiteBoardFilter verwendet werden.
   */
  public boolean isEnableWhiteBoardFilter()
  {
    return enableWhiteBoardFilter;
  }

  public void setEnableWhiteBoardFilter(final boolean enableWhiteBoardFilter)
  {
    this.enableWhiteBoardFilter = enableWhiteBoardFilter;
  }

  /**
   * Valid FileFormat: jpg, jpeg, gif, png. Wird Variable leer gelassen, können alle Formate ausgewählt werden. Wird
   * Variable mit gültigem Wert befüllt, können Images nur im jeweligen Dateiformat erzeugt werden.
   */
  public String getFileFormat()
  {
    return fileFormat;
  }

  public void setFileFormat(final String fileFormat)
  {
    if (StringHelper.isIn(fileFormat, "png", "gif", "jpg", "jpeg") == true) {
      this.fileFormat = fileFormat;
    } else {
      log.error("Unsupported file format: " + fileFormat);
    }
  }

  /**
   * Valid language: DE, EN Wird Variable leer gelassen, wird die language des Users verwendet.
   */
  public String getDefaultLanguage()
  {
    if (defaultLanguage != null) {
      return defaultLanguage;
    }
    return ThreadLocalUserContext.getLocale().getCountry();
  }

  public void setDefaultLanguage(final String defaultLanguage)
  {
    if (StringHelper.isIn(defaultLanguage, "EN", "DE") == true) {
      this.defaultLanguage = defaultLanguage;
    } else {
      log.error("Unsupported language: " + defaultLanguage);
    }
  }

  /**
   * @return false
   * @see org.projectforge.web.wicket.AbstractUnsecureBasePage#isBookmarkLinkIconVisible()
   */
  //  @Override
  //  protected boolean isBookmarkLinkIconVisible()
  //  {
  //    return false;
  //  }

  private ImageCropperPage appendVar(final StringBuffer buf, final String variable, final Object value)
  {
    buf.append("var ").append(variable).append(" = ");
    if (value == null) {
      buf.append("null");
    } else if (value instanceof String) {
      buf.append("\"").append(value).append("\"");
    } else {
      buf.append(value);
    }
    buf.append(";\n");
    return this;
  }

  @Override
  protected String getTitle()
  {
    return "ImageCropper";
  }
}
