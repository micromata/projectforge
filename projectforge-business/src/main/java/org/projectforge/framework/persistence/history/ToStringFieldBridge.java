package org.projectforge.framework.persistence.history;

import org.apache.commons.lang.ObjectUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * Calls just ObjectUtils.toString()
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class ToStringFieldBridge implements FieldBridge
{
  @Override
  public void set(String name, Object value, Document document, LuceneOptions luceneOptions)
  {
    if (value == null) {
      return;
    }
    document.add(new StringField(name, ObjectUtils.toString(value), Store.NO));
  }
}