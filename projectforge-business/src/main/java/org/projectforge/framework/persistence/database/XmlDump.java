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

package org.projectforge.framework.persistence.database;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.PostConstruct;
import javax.persistence.Transient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;
import org.hibernate.EmptyInterceptor;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.projectforge.business.fibu.AbstractRechnungDO;
import org.projectforge.business.fibu.AbstractRechnungsPositionDO;
import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.AuftragsPositionDO;
import org.projectforge.business.fibu.EingangsrechnungDO;
import org.projectforge.business.fibu.EingangsrechnungsPositionDO;
import org.projectforge.business.fibu.EmployeeSalaryDO;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.RechnungDO;
import org.projectforge.business.fibu.RechnungsPositionDO;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost2ArtDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.KostZuweisungDO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.user.UserPrefDOXmlDumpHook;
import org.projectforge.business.user.UserXmlPreferencesDao;
import org.projectforge.business.user.UserXmlPreferencesXmlDumpHook;
import org.projectforge.common.BeanHelper;
import org.projectforge.framework.access.AccessEntryDO;
import org.projectforge.framework.access.GroupTaskAccessDO;
import org.projectforge.framework.configuration.ConfigurationDOXmlDumpHook;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;
import org.projectforge.framework.persistence.hibernate.HibernateCompatUtils;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.UserPrefDO;
import org.projectforge.framework.persistence.user.entities.UserPrefEntryDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;
import org.projectforge.framework.persistence.xstream.HibernateXmlConverter;
import org.projectforge.framework.persistence.xstream.XStreamSavingConverter;
import org.projectforge.framework.xstream.XStreamHelper;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.PluginAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.stereotype.Service;

import com.thoughtworks.xstream.XStream;

import de.micromata.genome.db.jpa.history.api.HistoryEntry;
import de.micromata.genome.jpa.metainf.EntityMetadata;

/**
 * Dumps and restores the data-base.
 * <p>
 * TODO RK delete this class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
@Deprecated
public class XmlDump
{
  private static final Logger log = Logger.getLogger(XmlDump.class);

  private static final String XML_DUMP_FILENAME = System.getProperty("user.home") + "/tmp/database-dump.xml.gz";

  @Autowired
  private HibernateTemplate hibernate;

  @Autowired
  UserRightService userRights;

  @Autowired
  UserXmlPreferencesDao userXmlPreferencesDao;

  @Autowired
  PluginAdminService pluginAdminService;

  private final List<XmlDumpHook> xmlDumpHooks = new LinkedList<XmlDumpHook>();

  @Autowired
  private PfEmgrFactory emf;

  @PostConstruct
  public void init()
  {
    this.registerHook(new UserXmlPreferencesXmlDumpHook());
    this.registerHook(new UserPrefDOXmlDumpHook());
    this.registerHook(new ConfigurationDOXmlDumpHook());
  }

  /**
   * TODO RK better also via Metadata. These classes are stored automatically because they're dependent.
   */
  private final Class<?>[] embeddedClasses = new Class<?>[] { UserRightDO.class, AuftragsPositionDO.class,
      EingangsrechnungsPositionDO.class, RechnungsPositionDO.class };

  public HibernateTemplate getHibernate()
  {
    Validate.notNull(hibernate);
    return hibernate;
  }

  public void registerHook(final XmlDumpHook xmlDumpHook)
  {
    for (final XmlDumpHook hook : xmlDumpHooks) {
      if (hook.getClass().equals(xmlDumpHook.getClass()) == true) {
        log.error("Can't register XmlDumpHook twice: " + xmlDumpHook);
        return;
      }
    }
    xmlDumpHooks.add(xmlDumpHook);
  }

  /**
   * @return Only for test cases.
   */
  public XStreamSavingConverter restoreDatabase()
  {
    try {
      return restoreDatabase(new InputStreamReader(new FileInputStream(XML_DUMP_FILENAME), "utf-8"));
    } catch (final UnsupportedEncodingException ex) {
      log.error(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    } catch (final FileNotFoundException ex) {
      log.error(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    }
  }

  /**
   * @param reader
   * @return Only for test cases.
   */
  public XStreamSavingConverter restoreDatabase(final Reader reader)
  {
    final List<AbstractPlugin> plugins = pluginAdminService.getActivePlugin();
    final XStreamSavingConverter xstreamSavingConverter = new XStreamSavingConverter()
    {

      @Override
      protected Serializable getOriginalIdentifierValue(final Object obj)
      {
        return HibernateUtils.getIdentifier(obj);
      }

      @Override
      public Serializable onBeforeSave(final Session session, final Object obj)
      {
        log.info("Object " + obj);
        if (obj instanceof PFUserDO) {
          final PFUserDO user = (PFUserDO) obj;
          return save(user, user.getRights());
        } else if (obj instanceof AbstractRechnungDO<?>) {

          final AbstractRechnungDO<? extends AbstractRechnungsPositionDO> rechnung = (AbstractRechnungDO<?>) obj;
          final List<? extends AbstractRechnungsPositionDO> positions = rechnung.getPositionen();
          final KontoDO konto = rechnung.getKonto();
          if (konto != null) {
            save(konto);
            rechnung.setKonto(null);
          }
          rechnung.setPositionen(null); // Need to nullable positions first (otherwise insert fails).
          final Serializable id = save(rechnung);
          if (konto != null) {
            rechnung.setKonto(konto);
          }
          if (positions != null) {
            for (final AbstractRechnungsPositionDO pos : positions) {
              if (pos.getKostZuweisungen() != null) {
                final List<KostZuweisungDO> zuweisungen = pos.getKostZuweisungen();
                pos.setKostZuweisungen(null); // Need to nullable first (otherwise insert fails).
                save(pos);
                if (pos instanceof RechnungsPositionDO) {
                  ((RechnungDO) rechnung).addPosition((RechnungsPositionDO) pos);
                } else {
                  ((EingangsrechnungDO) rechnung).addPosition((EingangsrechnungsPositionDO) pos);
                }
                if (zuweisungen != null) {
                  for (final KostZuweisungDO zuweisung : zuweisungen) {
                    pos.addKostZuweisung(zuweisung);
                    save(zuweisung);
                  }
                }
              }
            }
          }
          return id;
        } else if (obj instanceof AuftragDO) {
          final AuftragDO auftrag = (AuftragDO) obj;
          return save(auftrag, auftrag.getPositionenIncludingDeleted());
        }
        if (plugins != null) {
          for (final AbstractPlugin plugin : plugins) {
            try {
              plugin.onBeforeRestore(this, obj);
            } catch (final Exception ex) {
              log.error("Error in Plugin while restoring object: " + ex.getMessage(), ex);
            }
          }
        }
        for (final XmlDumpHook xmlDumpHook : xmlDumpHooks) {
          try {
            xmlDumpHook.onBeforeRestore(userXmlPreferencesDao, this, obj);
          } catch (final Exception ex) {
            log.error("Error in XmlDumpHook while restoring object: " + ex.getMessage(), ex);
          }
        }
        return super.onBeforeSave(session, obj);
      }

      /**
       * @see org.projectforge.framework.persistence.xstream.XStreamSavingConverter#onAfterSave(java.lang.Object,
       *      java.io.Serializable)
       */
      @Override
      public void onAfterSave(final Object obj, final Serializable id)
      {
        if (plugins != null) {
          for (final AbstractPlugin plugin : plugins) {
            plugin.onAfterRestore(this, obj, id);
          }
        }
      }
    };
    // UserRightDO is inserted on cascade while inserting PFUserDO.
    xstreamSavingConverter.appendIgnoredObjects(embeddedClasses);
    // automatically detect insert order.
    List<EntityMetadata> ents = emf.getMetadataRepository().getTableEntities();
    List<Class<?>> classList = ents.stream().map((e) -> e.getJavaType()).collect(Collectors.toList());
    // first entities with now deps
    Collections.reverse(classList);

    xstreamSavingConverter.appendOrderedType(PFUserDO.class, GroupDO.class, TaskDO.class, KundeDO.class,
        ProjektDO.class, Kost1DO.class,
        Kost2ArtDO.class, Kost2DO.class, AuftragDO.class, //
        RechnungDO.class, EingangsrechnungDO.class, EmployeeSalaryDO.class, KostZuweisungDO.class, //
        UserPrefEntryDO.class, UserPrefDO.class, //
        AccessEntryDO.class, GroupTaskAccessDO.class, ConfigurationDO.class);
    xstreamSavingConverter.appendOrderedType(classList.toArray(new Class<?>[] {}));

    //    if (plugins != null) {
    //      for (final AbstractPlugin plugin : plugins) {
    //        xstreamSavingConverter.appendOrderedType(plugin.getPersistentEntities());
    //      }
    //    }
    Session session = null;
    try {
      final SessionFactory sessionFactory = hibernate.getSessionFactory();
      session = HibernateCompatUtils.openSession(sessionFactory, EmptyInterceptor.INSTANCE);
      session.setFlushMode(FlushMode.AUTO);
      final XStream xstream = XStreamHelper.createXStream();
      xstream.setMode(XStream.ID_REFERENCES);
      xstreamSavingConverter.setSession(session);
      xstream.registerConverter(xstreamSavingConverter, 10);
      xstream.registerConverter(new UserRightIdSingleValueConverter(userRights), 20);
      xstream.registerConverter(new UserPrefAreaSingleValueConverter(), 19);
      // alle Objekte Laden und speichern
      xstream.fromXML(reader);

      xstreamSavingConverter.saveObjects();
    } catch (final Exception ex) {
      log.error(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    } finally {
      IOUtils.closeQuietly(reader);
      if (session != null) {
        session.close();
      }
    }
    return xstreamSavingConverter;
  }

  /**
   * @return Only for test cases.
   */
  public XStreamSavingConverter restoreDatabaseFromClasspathResource(final String path, final String encoding)
  {
    final ClassPathResource cpres = new ClassPathResource(path);
    Reader reader;
    try {
      InputStream in;
      if (path.endsWith(".gz") == true) {
        in = new GZIPInputStream(cpres.getInputStream());
      } else {
        in = cpres.getInputStream();
      }
      reader = new InputStreamReader(in, encoding);
    } catch (final IOException ex) {
      log.error(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    }
    return restoreDatabase(reader);
  }

  public void dumpDatabase()
  {
    dumpDatabase(XML_DUMP_FILENAME, "utf-8");
  }

  /**
   * @param filename virtual filename: If the filename suffix is "gz" then the dump will be compressed.
   * @param out
   */
  public void dumpDatabase(final String filename, final OutputStream out)
  {
    final HibernateXmlConverter converter = new HibernateXmlConverter()
    {
      @Override
      protected void init(final XStream xstream)
      {
        xstream.omitField(AbstractBaseDO.class, "minorChange");
        xstream.omitField(AbstractBaseDO.class, "selected");
        xstream.registerConverter(new UserRightIdSingleValueConverter(userRights), 20);
        xstream.registerConverter(new UserPrefAreaSingleValueConverter(), 19);
      }
    };
    converter.setHibernate(hibernate);
    converter.appendIgnoredTopLevelObjects(embeddedClasses);
    Writer writer = null;
    GZIPOutputStream gzipOut = null;
    try {
      if (filename.endsWith(".gz") == true) {
        gzipOut = new GZIPOutputStream(out);
        writer = new OutputStreamWriter(gzipOut, "utf-8");
      } else {
        writer = new OutputStreamWriter(out, "utf-8");
      }
      converter.dumpDatabaseToXml(writer, true); // history=false, preserveIds=true
    } catch (final IOException ex) {
      log.error(ex.getMessage(), ex);
    } finally {
      IOUtils.closeQuietly(gzipOut);
      IOUtils.closeQuietly(writer);
    }
  }

  public void dumpDatabase(final String path, final String encoding)
  {
    OutputStream out = null;
    try {
      out = new FileOutputStream(path);
      dumpDatabase(path, out);
    } catch (final IOException ex) {
      log.error(ex.getMessage(), ex);
    } finally {
      IOUtils.closeQuietly(out);
    }
  }

  /**
   * Verify the imported dump.
   *
   * @return Number of checked objects. This number is negative if any error occurs (at least one object wasn't imported
   * successfully).
   */
  public int verifyDump(final XStreamSavingConverter xstreamSavingConverter)
  {
    final SessionFactory sessionFactory = hibernate.getSessionFactory();
    Session session = null;
    boolean hasError = false;
    try {
      session = HibernateCompatUtils.openSession(sessionFactory, EmptyInterceptor.INSTANCE);
      session.setDefaultReadOnly(true);
      int counter = 0;
      for (final Map.Entry<Class<?>, List<Object>> entry : xstreamSavingConverter.getAllObjects().entrySet()) {
        final List<Object> objects = entry.getValue();
        final Class<?> entityClass = entry.getKey();
        if (objects == null) {
          continue;
        }
        for (final Object obj : objects) {
          if (HibernateUtils.isEntity(obj.getClass()) == false) {
            continue;
          }
          final Serializable id = HibernateUtils.getIdentifier(obj);
          if (id == null) {
            // Can't compare this object without identifier.
            continue;
          }
          // log.info("Testing object: " + obj);
          final Object databaseObject = session.get(entityClass, id, LockOptions.READ);
          Hibernate.initialize(databaseObject);
          final boolean equals = equals(obj, databaseObject, true);
          if (equals == false) {
            log.error("Object not sucessfully imported! xml object=[" + obj + "], data base=[" + databaseObject + "]");
            hasError = true;
          }
          ++counter;
        }
      }

      for (final HistoryEntry historyEntry : xstreamSavingConverter.getHistoryEntries()) {
        final Class<?> type = xstreamSavingConverter.getClassFromHistoryName(historyEntry.getEntityName());
        final Object o = type != null ? session.get(type, historyEntry.getEntityId()) : null;
        if (o == null) {
          log.warn("A corrupted history entry found (entity of class '"
              + historyEntry.getEntityName()
              + "' with id "
              + historyEntry.getEntityId()
              + " not found: "
              + historyEntry
              + ". This doesn't affect the functioning of ProjectForge, this may result in orphaned history entries.");
          hasError = true;
        }
        ++counter;
      }
      if (hasError == true) {
        log.fatal(
            "*********** A inconsistency in the import was found! This may result in a data loss or corrupted data! Please retry the import. "
                + counter
                + " entries checked.");
        return -counter;
      }
      log.info("Data-base import successfully verified: " + counter + " entries checked.");
      return counter;
    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  /**
   * @param o1
   * @param o2
   * @param logDifference If true than the difference is logged.
   * @return True if the given objects are equal.
   */
  private boolean equals(final Object o1, final Object o2, final boolean logDifference)
  {
    if (o1 == null) {
      final boolean equals = (o2 == null);
      if (equals == false && logDifference == true) {
        log.error("Value 1 is null and value 2 is " + o2);
      }
      return equals;
    } else if (o2 == null) {
      if (logDifference == true) {
        log.error("Value 2 is null and value 1 is " + o1);
      }
      return false;
    }
    final Class<?> cls1 = o1.getClass();
    final Field[] fields = cls1.getDeclaredFields();
    AccessibleObject.setAccessible(fields, true);
    for (final Field field : fields) {
      if (accept(field) == false) {
        continue;
      }
      try {
        final Object fieldValue1 = getValue(o1, o2, field);
        final Object fieldValue2 = getValue(o2, o1, field);
        if (field.getType().isPrimitive() == true) {
          if (ObjectUtils.equals(fieldValue2, fieldValue1) == false) {
            if (logDifference == true) {
              log.error("Field is different: " + field.getName() + "; value 1 '" + fieldValue1 + "' 2 '"
                  + fieldValue2 + "'.");
            }
            return false;
          }
          continue;
        } else if (fieldValue1 == null) {
          if (fieldValue2 != null) {
            if (fieldValue2 instanceof Collection<?>) {
              if (CollectionUtils.isEmpty((Collection<?>) fieldValue2) == true) {
                // null is equals to empty collection in this case.
                return true;
              }
            }
            if (logDifference == true) {
              log.error("Field '" + field.getName() + "': value 1 '" + fieldValue1 + "' is different from value 2 '"
                  + fieldValue2 + "'.");
            }
            return false;
          }
        } else if (fieldValue2 == null) {
          if (fieldValue1 != null) {
            if (logDifference == true) {
              log.error("Field '" + field.getName() + "': value 1 '" + fieldValue1 + "' is different from value 2 '"
                  + fieldValue2 + "'.");
            }
            return false;
          }
        } else if (fieldValue1 instanceof Collection<?>) {
          final Collection<?> col1 = (Collection<?>) fieldValue1;
          final Collection<?> col2 = (Collection<?>) fieldValue2;
          if (col1.size() != col2.size()) {
            if (logDifference == true) {
              log.error("Field '"
                  + field.getName()
                  + "': colection's size '"
                  + col1.size()
                  + "' is different from collection's size '"
                  + col2.size()
                  + "'.");
            }
            return false;
          }
          if (equals(field, col1, col2, logDifference) == false || equals(field, col2, col1, logDifference) == false) {
            return false;
          }
        } else if (HibernateUtils.isEntity(fieldValue1.getClass()) == true) {
          if (fieldValue2 == null
              || ObjectUtils.equals(HibernateUtils.getIdentifier(fieldValue1),
              HibernateUtils.getIdentifier(fieldValue2)) == false) {
            if (logDifference == true) {
              log.error("Field '"
                  + field.getName()
                  + "': Hibernate object id '"
                  + HibernateUtils.getIdentifier(fieldValue1)
                  + "' is different from id '"
                  + HibernateUtils.getIdentifier(fieldValue2)
                  + "'.");
            }
            return false;
          }
        } else if (fieldValue1 instanceof BigDecimal) {
          if (fieldValue2 == null || ((BigDecimal) fieldValue1).compareTo((BigDecimal) fieldValue2) != 0) {
            if (logDifference == true) {
              log.error("Field '" + field.getName() + "': value 1 '" + fieldValue1 + "' is different from value 2 '"
                  + fieldValue2 + "'.");
            }
            return false;
          }
        } else if (fieldValue1.getClass().isArray() == true) {
          if (ArrayUtils.isEquals(fieldValue1, fieldValue2) == false) {
            if (logDifference == true) {
              log.error("Field '" + field.getName() + "': value 1 '" + fieldValue1 + "' is different from value 2 '"
                  + fieldValue2 + "'.");
            }
            return false;
          }
        } else if (ObjectUtils.equals(fieldValue2, fieldValue1) == false) {
          if (logDifference == true) {
            log.error("Field '" + field.getName() + "': value 1 '" + fieldValue1 + "' is different from value 2 '"
                + fieldValue2 + "'.");
          }
          return false;
        }
      } catch (final IllegalAccessException ex) {
        throw new InternalError("Unexpected IllegalAccessException: " + ex.getMessage());
      }
    }
    return true;
  }

  /**
   * Tests if every entry of col1 is found as equals entry in col2. You need to call this method twice with swapped
   * params for being sure of equality!
   *
   * @param col1
   * @param col2
   * @return
   */
  private boolean equals(final Field field, final Collection<?> col1, final Collection<?> col2,
      final boolean logDifference)
  {
    for (final Object colVal1 : col1) {
      boolean equals = false;
      for (final Object colVal2 : col2) {
        if (equals(colVal1, colVal2, false) == true) {
          equals = true; // Equal object found.
          break;
        }
      }
      if (equals == false) {
        if (logDifference == true) {
          log.error("Field '" + field.getName() + "': value '" + colVal1 + "' not found in other collection.");
        }
        return false;
      }
    }
    return true;
  }

  /**
   * @param obj
   * @param compareObj Only need for @Transient (because Javassist proxy doesn't have this annotion).
   * @param field
   * @return
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  private Object getValue(final Object obj, final Object compareObj, final Field field) throws IllegalArgumentException,
      IllegalAccessException
  {
    Object val = null;
    final Method getter = BeanHelper.determineGetter(obj.getClass(), field.getName());
    final Method getter2 = BeanHelper.determineGetter(compareObj.getClass(), field.getName());
    if (getter != null
        && getter.isAnnotationPresent(Transient.class) == false
        && getter2 != null
        && getter2.isAnnotationPresent(Transient.class) == false) {
      val = BeanHelper.invoke(obj, getter);
    }
    if (val == null) {
      val = field.get(obj);
    }
    return val;
  }

  /**
   * @param field
   * @return true, if the given field should be compared.
   */
  protected boolean accept(final Field field)
  {
    if (field.getName().indexOf(ClassUtils.INNER_CLASS_SEPARATOR_CHAR) != -1) {
      // Reject field from inner class.
      return false;
    }
    if (field.getName().equals("handler") == true) {
      // Handler of Javassist proxy should be ignored.
      return false;
    }
    if (Modifier.isTransient(field.getModifiers()) == true) {
      // transients.
      return false;
    }
    if (Modifier.isStatic(field.getModifiers()) == true) {
      // transients.
      return false;
    }
    return true;
  }
}
