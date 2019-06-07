/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.jpa.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.IndexDescriptor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.PropertyDescriptor;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.micromata.genome.jpa.metainf.ColumnMetadata;
import de.micromata.genome.util.runtime.RuntimeIOException;

/**
 * Debug utilities for Hibernate Search.
 * 
 * TODO RK make service interface.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@Service
public class LuceneServiceImpl
{
  @Autowired
  private PfEmgrFactory emf;

  public Set<Class<?>> getSearchClasses()
  {
    return emf.runInTrans((emgr) -> {
      FullTextEntityManager femg = emgr.getFullTextEntityManager();
      SearchFactory sf = femg.getSearchFactory();
      Set<Class<?>> itypes = sf.getIndexedTypes();
      return itypes;
    });
  }

  private String[] splitFieldList(String fieldList)
  {
    return StringUtils.split(fieldList, ", ");
  }

  public String searchViaBaseDao(Class<?> entityClass, String search, String fieldList)
  {
    //    Object dao = fallbackBaseDaoService.getBaseDaoForEntity((ExtendedBaseDO<Integer>)entityClass);
    return "";
  }

  public String searchViaHibernateSearch(Class<?> entityClass, String search, String fieldList)
  {

    List<?> list = emf.runInTrans((emgr) -> {
      String[] fl = StringUtils.split(fieldList, ", ");
      if (fl == null) {
        fl = new String[] {};
      }
      return emgr.searchWildcardDetached(StringUtils.defaultString(search), entityClass, fl);
    });
    StringBuilder sb = new StringBuilder();
    sb.append("Found: " + list.size()).append("<br/>\n");
    for (Object ob : list) {
      sb.append("<br/>------------------------------------------------------<br/>");
      sb.append(ob);
    }
    return sb.toString();
  }

  /**
   * Returns html
   * 
   * @param entityClass
   * @param search
   * @return
   */
  public String searchSimple(Class<?> entityClass, String search, String fieldList)
  {
    StringBuilder sb = new StringBuilder();
    String rsearch = StringUtils.isBlank(search) ? "*" : search;

    return emf.runInTrans((emgr) -> {

      FullTextEntityManager femg = emgr.getFullTextEntityManager();
      SearchFactory sf = femg.getSearchFactory();
      String[] searchFields;
      if (StringUtils.isNotBlank(fieldList) == true) {
        searchFields = splitFieldList(fieldList);
      } else {
        searchFields = getSearchFieldsForEntity(entityClass);
      }
      final MultiFieldQueryParser parser = new MultiFieldQueryParser(searchFields, new ClassicAnalyzer());
      parser.setAllowLeadingWildcard(true);
      try {
        Query query = parser.parse(rsearch);
        IndexReaderAccessor ia = sf.getIndexReaderAccessor();

        try (IndexReader ir = ia.open(entityClass)) {
          IndexSearcher is = new IndexSearcher(ir);

          TopDocs ret = is.search(query, 1000);
          sb.append("found: " + ret.totalHits).append("<br/>\n");

          for (ScoreDoc sdoc : ret.scoreDocs) {
            sb.append("===================================================================").append("<br/>\n");
            sb.append(StringEscapeUtils.escapeHtml4(sdoc.toString())).append("  ");
            Document document = is.doc(sdoc.doc);
            sb.append("LuceneDocument: ");
            renderDocument(document, sb);
            IndexableField id = document.getField("id");
            ColumnMetadata idCol = emf.getMetadataRepository().getEntityMetadata(entityClass).getIdColumn();
            if (id == null) {
              id = document.getField("pk");
            }
            String snval = id.stringValue();
            Class<?> idclazz = idCol.getJavaType();
            Serializable entityPk;
            if (idclazz.isAssignableFrom(Long.class)) {
              entityPk = Long.parseLong(snval);
            } else {
              entityPk = Integer.parseInt(snval);
            }
            Object entity = emgr.findByPkDetached(entityClass, entityPk);
            String osdesc = ToStringBuilder.reflectionToString(entity, ToStringStyle.MULTI_LINE_STYLE, false);
            osdesc = StringEscapeUtils.escapeHtml4(osdesc);
            osdesc = StringUtils.replace(osdesc, "\n", "<br/>\n<nbsp/><nbsp/>");
            sb.append(osdesc).append("<br/>\n");
            sb.append("<br/>\n");
          }

        } catch (IOException ex) {
          throw new RuntimeIOException(ex);
        }
      } catch (org.apache.lucene.queryparser.classic.ParseException ex) {
        throw new RuntimeException(ex);
      }
      return sb.toString();
    });
  }

  private void renderDocument(Document document, StringBuilder sb)
  {
    sb.append("<br/>\n").append("----------------------------------------------------------").append("<br/>\n");
    sb.append(StringEscapeUtils.escapeHtml4(document.toString())).append("<br/>\n");
    for (IndexableField field : document.getFields()) {
      sb.append("  ").append(field.name()).append(": ").append(field.stringValue()).append("<br/>\n");
    }
    sb.append("----------------------------------------------------------").append("<br/>\n");
  }

  public String[] getSearchFieldsForEntity(Class<?> entityClass)
  {
    String[] ret = emf.getSearchFieldsForEntity(entityClass).keySet().toArray(new String[] {});

    List<String> list = emf.runInTrans((emgr) -> {
      FullTextEntityManager femg = emgr.getFullTextEntityManager();
      SearchFactory sf = femg.getSearchFactory();
      IndexedTypeDescriptor itd = sf.getIndexedTypeDescriptor(entityClass);
      return itd.getIndexedProperties().stream().map((desc) -> desc.getName()).collect(Collectors.toList());
    });
    if (ret.length <= list.size()) {
      return list.toArray(new String[] {});
    }
    return ret;
  }

  public String getIndexDescription(Class<?> entityClass)
  {
    StringBuilder sb = new StringBuilder();
    emf.runInTrans((emgr) -> {
      sb.append("class: ").append(entityClass.getName()).append("\n");

      FullTextEntityManager femg = emgr.getFullTextEntityManager();
      SearchFactory sf = femg.getSearchFactory();
      IndexedTypeDescriptor itd = sf.getIndexedTypeDescriptor(entityClass);
      List<String> fields = itd.getIndexedProperties().stream().map((desc) -> desc.getName())
          .collect(Collectors.toList());
      sb.append("\nFields: ").append(StringUtils.join(fields, ", ")).append("\n");

      IndexedTypeDescriptor descr = sf.getIndexedTypeDescriptor(entityClass);
      sb.append("\nIndexedTypeDescriptor: indexed: ").append(descr.isIndexed()).append("\nFields:\n");
      for (FieldDescriptor field : descr.getIndexedFields()) {
        sb.append("  ").append(field).append("<br?\n");
      }
      sb.append("\nProperties: \n");
      for (PropertyDescriptor ip : descr.getIndexedProperties()) {
        sb.append("  ").append(ip).append("\n");
      }

      sb.append("\nIndexe: \n");
      for (IndexDescriptor ides : descr.getIndexDescriptors()) {
        sb.append("  ").append(ides).append("\n");
      }

      String[] sfields = getSearchFieldsForEntity(entityClass);
      sb.append("\nSearchFields: ").append(StringUtils.join(sfields, ",")).append("\n");
      return null;
    });
    return sb.toString();
  }

  public void reindex(Class<?> entityClass)
  {
    emf.runInTrans((emgr) -> {

      FullTextEntityManager femg = emgr.getFullTextEntityManager();
      femg.purgeAll(entityClass);

      List<?> allentities = emgr.selectAllAttached(entityClass);
      for (Object ent : allentities) {
        femg.index(ent);
      }
      femg.flushToIndexes();
      //      emgr.detach(allentities);

      return null;
    });
  }
}
