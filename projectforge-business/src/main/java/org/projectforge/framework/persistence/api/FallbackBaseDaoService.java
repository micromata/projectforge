package org.projectforge.framework.persistence.api;

/**
 * Bridge to Fallback BaseDaos
 * 
 * 
 * TODO RK check with class Registry.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public interface FallbackBaseDaoService
{
  <ENT extends ExtendedBaseDO<Integer>> BaseDao<ENT> getBaseDaoForEntity(Class<ENT> ent);
}
