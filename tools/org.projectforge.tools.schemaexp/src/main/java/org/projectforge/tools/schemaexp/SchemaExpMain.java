package org.projectforge.tools.schemaexp;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.continuousdb.DatabaseSupport;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlDumpService.RestoreMode;
import de.micromata.genome.util.runtime.LocalSettingsEnv;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class SchemaExpMain
{

  private static void help()
  {
    System.out.println("[-imp|-exp] [OPTIONS] <in or outfile>");
    System.out.println("or");
    System.out.println("-cleardb");
    System.out.println("  OPTIONS -cleardb  empty all tables before import");
    //    System.out.println("  OPTIONS: [-insertnew|-overwrite|-insertall] required for -imp");

  }

  enum Operation
  {
    Import, Export
  }

  static Operation op = null;
  static boolean clearDb = false;
  // currently only one supported
  static RestoreMode restoreMode = RestoreMode.InsertAll;

  private static boolean parseOption(String[] args, int pos)
  {
    if (args.length <= pos) {
      return false;
    }
    if (StringUtils.equalsIgnoreCase(args[pos], "-imp") == true) {
      op = Operation.Import;
      return true;
    } else if (StringUtils.equalsIgnoreCase(args[pos], "-exp") == true) {
      op = Operation.Export;
      return true;
    } else if (StringUtils.equalsIgnoreCase(args[pos], "-cleardb") == true) {
      clearDb = true;
      return true;
    } else if (StringUtils.equalsIgnoreCase(args[pos], "-insertnew") == true) {
      restoreMode = RestoreMode.InsertNew;
      return true;
    } else if (StringUtils.equalsIgnoreCase(args[pos], "-overwrite") == true) {
      restoreMode = RestoreMode.OverWrite;
      return true;
    } else if (StringUtils.equalsIgnoreCase(args[pos], "-insertall") == true) {
      restoreMode = RestoreMode.InsertAll;
      return true;
    }
    return false;
  }

  public static void main(String[] args)
  {

    if (args.length < 1) {
      help();
      return;
    }
    LocalSettingsEnv.get();
    ApplicationContext context = initApplicatinContext();
    SchemaExpService ms = context.getBean(SchemaExpService.class);

    int pos = 0;
    try {
      while (parseOption(args, pos++)) {

      }
      --pos;
    } catch (IllegalArgumentException ex) {
      System.out.println(ex.getMessage());
      help();
      return;
    }
    if (op == null) {
      if (clearDb == true) {
        ms.clearDb();
        return;
      }
      help();
      return;
    }
    if (op == Operation.Import && restoreMode == null) {
      help();
      return;
    }

    if (args.length <= pos) {
      help();
      return;
    }

    String fileArg = args[pos];

    try {
      switch (op) {
        case Export:
          ms.doExport(fileArg);
          break;
        case Import:
          ms.doImport(clearDb, fileArg, restoreMode);
          break;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private static ApplicationContext initApplicatinContext()
  {
    ApplicationContext context = new AnnotationConfigApplicationContext(SchemaExpContext.class);
    TenantRegistryMap.getInstance().setApplicationContext(context);
    //    GlobalConfiguration._init4TestMode();
    DatabaseSupport.setInstance(new DatabaseSupport(HibernateUtils.getDialect()));
    return context;
  }
}
