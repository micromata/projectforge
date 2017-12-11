package org.projectforge.web.wicket;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.wicket.Application;
import org.apache.wicket.page.IManageablePage;
import org.apache.wicket.pageStore.IPageStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A custom IPageStore implementation, that keeps pages in memory without serialization.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class InMemoryPageStore implements IPageStore
{

  /**
   * The logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(InMemoryPageStore.class);

  /**
   * The configured cache size (per session).
   */
  private final int cacheSize;

  /**
   * Data structure to store pages per sessionId and per pageId.
   */
  private final Map<String, Map<Integer, SoftReference<IManageablePage>>> cache;

  /**
   * Map that holds the indices of last/least recently used page ids. The most recently used page id is in the tail, the
   * least recently used is in the head.
   */
  private final Map<String, Queue<Integer>> indices;

  /**
   * Constructor.
   */
  public InMemoryPageStore()
  {
    this.cacheSize = Application.get().getStoreSettings().getInmemoryCacheSize();
    this.cache = new HashMap<>();
    this.indices = new HashMap<>();
  }

  @Override
  public void destroy()
  {
    synchronized (cache) {
      cache.clear();
      indices.clear();
    }
  }

  @Override
  public IManageablePage getPage(String sessionId, int pageId)
  {
    Map<Integer, SoftReference<IManageablePage>> sessionCache = getSessionCache(sessionId);
    SoftReference<IManageablePage> reference = sessionCache.get(pageId);
    if (reference == null) {
      return null;
    }

    IManageablePage page = reference.get();
    Queue<Integer> sessionIndex = getSessionIndex(sessionId);

    synchronized (sessionIndex) {
      updateIndex(sessionId, pageId);
      return page;
    }
  }

  @Override
  public void removePage(String sessionId, int pageId)
  {
    Map<Integer, SoftReference<IManageablePage>> sessionCache = getSessionCache(sessionId);
    if (sessionCache != null) {
      synchronized (sessionCache) {
        sessionCache.remove(pageId);
        getSessionIndex(sessionId).remove(pageId);
      }
    }
  }

  @Override
  public void storePage(String sessionId, IManageablePage page)
  {
    Map<Integer, SoftReference<IManageablePage>> sessionCache = getSessionCache(sessionId);
    SoftReference<IManageablePage> reference = new SoftReference<>(page);

    synchronized (sessionCache) {
      sessionCache.put(page.getPageId(), reference);
      updateIndex(sessionId, page.getPageId());

      if (sessionCache.size() > cacheSize) {
        // Remove oldest page
        Integer oldestPageId = getOldestPageId(sessionId);
        removePage(sessionId, oldestPageId);
      }
    }
  }

  @Override
  public void unbind(String sessionId)
  {
    synchronized (getSessionCache(sessionId)) {
      cache.remove(sessionId);
      indices.remove(sessionId);
    }
  }

  @Override
  public Serializable prepareForSerialization(String sessionId, Serializable page)
  {
    throw new UnsupportedOperationException(getClass() + " does not support serialization");
  }

  @Override
  public Object restoreAfterSerialization(Serializable serializable)
  {
    throw new UnsupportedOperationException(getClass() + " does not support serialization");
  }

  @Override
  public IManageablePage convertToPage(Object page)
  {
    if (page instanceof IManageablePage) {
      return (IManageablePage) page;
    }
    logger.warn("page is not an instance of " + IManageablePage.class);
    return null;
  }

  /**
   * Returns the cache for stored pages for the given session id.
   *
   * @param sessionId the session id
   * @return the cache for stored pages for this session
   */
  private Map<Integer, SoftReference<IManageablePage>> getSessionCache(String sessionId)
  {
    Map<Integer, SoftReference<IManageablePage>> sessionCache = cache.get(sessionId);
    if (sessionCache == null) {
      sessionCache = new ConcurrentHashMap<>();
      cache.put(sessionId, sessionCache);
    }
    return sessionCache;
  }

  /**
   * Returns the index queue of page ids for the given session id. The ordering is from least recently used (head) to
   * most recently used (tail).
   *
   * @param sessionId the session id
   * @return index of least recently used pages for the given session id
   */
  private Queue<Integer> getSessionIndex(String sessionId)
  {
    Queue<Integer> sessionIndex = indices.get(sessionId);
    if (sessionIndex == null) {
      sessionIndex = new ConcurrentLinkedQueue<>();
      indices.put(sessionId, sessionIndex);
    }
    return sessionIndex;
  }

  /**
   * Returns the oldest (i.e. least recently used) page id for the given session id.
   *
   * @param sessionId the session id
   * @return id of the least recently used page for the given session id
   */
  protected Integer getOldestPageId(String sessionId)
  {
    return getSessionIndex(sessionId).peek();
  }

  /**
   * Updates the index for the given sessionId and pageId. This will remove the pageId from the queue and reinsert it
   * (at the end).
   *
   * @param sessionId the session id
   * @param pageId    the page id
   */
  private void updateIndex(String sessionId, Integer pageId)
  {
    Queue<Integer> index = getSessionIndex(sessionId);
    index.remove(pageId);
    index.offer(pageId);
  }

  /**
   * Returns the page cache, for testing purposes.
   *
   * @return the page cache
   */
  protected Map<String, Map<Integer, SoftReference<IManageablePage>>> getCache()
  {
    return cache;
  }

}
