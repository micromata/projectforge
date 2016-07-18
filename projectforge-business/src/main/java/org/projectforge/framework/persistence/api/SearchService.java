package org.projectforge.framework.persistence.api;

import java.util.List;

import org.projectforge.framework.access.AccessException;

/**
 * Base interface to search
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public interface SearchService
{
  public String[] getSearchFields(Class<? extends BaseDO<?>> entClass);

  <ENT extends ExtendedBaseDO<Integer>> List<ENT> getList(final QueryFilter filter, Class<ENT> entClazz)
      throws AccessException;
}
