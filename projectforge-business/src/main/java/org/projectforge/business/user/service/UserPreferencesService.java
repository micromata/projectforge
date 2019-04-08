package org.projectforge.business.user.service;

public interface UserPreferencesService {
  /**
   * Stores the given value for the current user.
   *
   * @param key
   * @param value
   * @param persistent If true, the object will be persisted in the database.
   * @see org.projectforge.business.user.UserXmlPreferencesCache#putEntry(Integer, String, Object, boolean)
   */
  public void putEntry(final String key, final Object value, final boolean persistent);

  /**
   * Gets the stored user preference entry.
   *
   * @param key
   * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
   * existing, otherwise null;
   * @see org.projectforge.business.user.UserXmlPreferencesCache#getEntry(Integer, String)
   */
  public Object getEntry(final String key);

  /**
   * Gets the stored user preference entry.
   *
   * @param key
   * @param expectedType Checks the type of the user pref entry (if found) and returns only this object if the object is
   *                     from the expected type, otherwise null is returned.
   * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
   * existing, otherwise null;
   * @see org.projectforge.business.user.UserXmlPreferencesCache#getEntry(Integer, String)
   */
  public <T> T getEntry(final Class<T> expectedType, final String key);

  /**
   * Removes the entry under the given key.
   *
   * @param key
   * @return The removed entry if found.
   */
  public Object removeEntry(final String key);
}
