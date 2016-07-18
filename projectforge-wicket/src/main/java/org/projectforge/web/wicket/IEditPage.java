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

package org.projectforge.web.wicket;

import org.apache.wicket.markup.html.WebPage;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.api.ICorePersistenceService;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;
import org.projectforge.web.task.TaskTreePage;

/**
 * IEditPages such as AbstractEditPage and AbstractMobileEditPage are supported by EditPageSupport.
 * 
 * @see EditPageSupport
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public interface IEditPage<O extends AbstractBaseDO<Integer>, D extends ICorePersistenceService<Integer, O>>
{
  /**
   * The flag already submitted should be set to false in onBeforeRender. It should be set to true if the user has
   * clicked a submit button. The goal is to detect multiple submits (double-click etc.),
   * 
   * @return
   */
  public boolean isAlreadySubmitted();

  /**
   * @param alreadySubmitted
   * @see #isAlreadySubmitted()
   */
  public void setAlreadySubmitted(final boolean alreadySubmitted);

  /**
   * Will be called before the data object will be stored. Does nothing at default. Any return value is not yet
   * supported.
   */
  public WebPage onSaveOrUpdate();

  /**
   * Will be called before the data object will be deleted or marked as deleted. Here you can add validation errors
   * manually. If this method returns a resolution then a redirect to this resolution without calling the baseDao
   * methods will done. <br/>
   * Here you can do validations with add(Global)Error or manipulate the data object before storing to the database etc.
   */
  public WebPage onDelete();

  /**
   * Will be called before the data object will be restored (undeleted). Here you can add validation errors manually. If
   * this method returns a resolution then a redirect to this resolution without calling the baseDao methods will done.
   * <br/>
   * Here you can do validations with add(Global)Error or manipulate the data object before storing to the database etc.
   */
  public WebPage onUndelete();

  /**
   * Will be called directly after storing the data object (insert, update, delete). If any page is returned then
   * proceed a redirect to this given page.
   */
  public WebPage afterSaveOrUpdate();

  /**
   * Will be called directly after storing the data object (insert). Any return value is not yet supported.
   */
  public WebPage afterSave();

  /**
   * Will be called directly after storing the data object (update).
   * 
   * @param modificationStatus MINOR or MAJOR, if the object was modified, otherwise NONE. If a not null web page is
   *          returned, then the web page will be set as response page.
   * @see BaseDao#update(ExtendedBaseDO)
   */
  public WebPage afterUpdate(ModificationStatus modificationStatus);

  /**
   * Will be called directly after deleting the data object (delete or update deleted=true). Any return value is not yet
   * supported.
   */
  public WebPage afterDelete();

  /**
   * Will be called directly after un-deleting the data object (update deleted=false). Any return value is not yet
   * supported.
   */
  public WebPage afterUndelete();

  public O getData();

  /**
   * If user tried to add a new object and an error was occurred the edit page is shown again and the object id is
   * cleared (set to null).
   */
  public void clearIds();

  /**
   * Sets the given page as response page. If the response page is of type AbstractListPage or TaskTreePage then the row
   * with the currently shown object is highlighted.
   * 
   * @param page
   * @see AbstractListPage#setHighlightedRowId(java.io.Serializable)
   * @see TaskTreePage#setHighlightedRowId(Integer)
   */
  public void setResponsePageAndHighlightedRow(final WebPage page);

  /**
   * Sets the standard response page (list page or return-to-page)
   */
  public void setResponsePage();

  /**
   * Checks weather the id of the data object is given or not.
   * 
   * @return true if the user wants to create a new data object or false for an already existing object.
   */
  public boolean isNew();
}
