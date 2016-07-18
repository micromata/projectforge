projectforge-continuous-db
==========================

Use this light-weight package for continuous delivery of your software (only apache-commons.jar and projectforge-common.jar is needed).

Any initial setup of the database as well as update-scripts are very easy to manage programmatically. The JPA annotations are supported,
therefore you may set-up a complete database schema with only a few lines of code.

## Initialization
You only need to get the current database dialect and the javax.sql.DataSource:
```java
DatabaseDialect dialect = DatabaseDialect.PostgreSQL; // Get the current dialect after start-up from Hibernate etc.
UpdaterConfiguration configuration = new UpdaterConfiguration().setDialect(dialect).setDataSource(dataSource);
DatabaseUpdateDao databaseUpdateDao = configuration.getDatabaseUpdateDao();
```

## Example: Initial setup of a database

The following example will create the whole database schema for the given entities. Please refer ```DemoMain.java``` for a first example.

```java
Class< ? >[] entities = new Class< ? >[] {
  UserDO.class,
  TaskDO.class, GroupDO.class, TaskDO.class, GroupTaskAccessDO.class,
  AccessEntryDO.class
};

if (databaseUpdateDao.doEntitiesExist(entities) == false) {
  // At least one table of the given entities doesn't exist. Create the missing tables:
  SchemaGenerator schemaGenerator = configuration.createSchemaGenerator().add(entities);
  schemaGenerator.createSchema();
  databaseUpdateDao.createMissingIndices(); // Create missing indices.
}
```

Please note: foreign-keys, one-to-many, many-to-one and many-to-many relations are supported as well as different column types. You may extend
this module very easy for support of more JPA annotations.

## Example: Update script

You may add columns to a table within your new version:

```java
if (databaseUpdateDao.do  TableAttributesExist(AddressDO.class, "birthday", "address") == false) {
  // One or both attributes don't yet exist, alter table to add the missing columns now:
  databaseUpdateDao.addTableAttributes(Address2DO.class, "birthday", "address");
  // Works also, if one of both attributes does already exist.
}
```

## Example: Migrating data

The following examle (of DemoMain.java) assumes that the type of a column has to be changed from ```VARCHAR``` to a decimal value:

```java
// Rename column:
databaseUpdateDao.renameTableAttribute("t_address", "amount", "old_amount");
// Create column of new type:
databaseUpdateDao.addTableAttributes(Address2DO.class, "amount");
// Convert types of any existing table entry:
List<DatabaseResultRow> rows = databaseUpdateDao.query("select pk, old_amount from t_address");
if (rows != null) {
  for (DatabaseResultRow row : rows) {
    Integer pk = (Integer)row.getEntry("pk").getValue();
    String amountAsString = (String)row.getEntry("old_amount").getValue();
    BigDecimal amount = null;
    if (amountAsString != null && amountAsString.trim().length() > 0) {
      // Do some conversion stuff:
      amount = new BigDecimal(amountAsString);
    }
    // Now update the column.
    databaseUpdateDao.update("update t_address set amount=? where pk=?", amount, pk);
  }
}
// Drop the old column:
databaseUpdateDao.dropTableAttribute("t_address", "old_amount");
```

## Manual creation without JPA annotations
You may create and update the database schema without JPA annotations:
```java
Table table = new Table("t_person");
if (databaseUpdateDao.doesExist(table) == false) {
  table.addAttribute(new TableAttribute("pk", TableAttributeType.INT).setPrimaryKey(true)) //
       .addAttribute(new TableAttribute("birthday", TableAttributeType.DATE)) //
       .addAttribute(new TableAttribute("name", TableAttributeType.VARCHAR, 100).setNullable(false)) //
       .addAttribute(new TableAttribute("user_id", TableAttributeType.INT) //
                    .setForeignTable("t_user").setForeignAttribute("pk"));
  databaseUpdateDao.createTable(table);
}
// Further examples:
databaseUpdateDao.alterTableColumnVarCharLength("t_person", "name", 255); // VARCHAR(100) -> VARCHAR(255)
```

## Using log4j or other logging frameworks
ProjectForge's continuous-db uses the Java standard logging as default. If you need log4j
you may initialize it before using continuous-db:
```java
Logger.setLoggerBridge(new LoggerBridgeLog4j()); // Before the first log message
```
You may use any other logging framework if you implement the LoggerBridge yourself.

## Advantage in comparison with other tools
You may organize your database inital and update scripts programmatically. Therefore it's very easy to do further migration
modifications during your update (e. g. merge columns etc.).  
It's easy to handle multiple database dialects (you write only code once which fits all database dialects). The database dialect of the destination system is used.

## UpdateEntries
For large projects with sub-modules (such as ProjectForge itself) it's recommended to organize setups by using the UpdateEntry class. Please refer http://www.projectforge.org/pf-en/Convenientupdates for seeing ProjectForge in action. The administration user is able to update the system by simply clicking the versioned update entries.
They have full control over the update process, no magic by automatical schema update of Hibernate etc.

## Restrictions
* Currently PostgreSQL and HSQLDB are supported. Please refer ```DatabaseSupport.java``` for adding new database dialects (this should be very easy).
* Currently only annotations of getter methods are supported (but it should be very easy to support also field annotations).
* The main JPA annotations are supported. For Hibernate specific annotations such as Type please refer the hook class ```TableAttributeHookImpl``` of the hibernate package.

## Using maven
### pom.xml (stable)
Cooming soon...

### pom.xml (SNAPSHOT)
```xml
<dependency>
  <groupId>org.projectforge</groupId>
  <artifactId>projectforge-continuous-db</artifactId>
  <version>5.1.1-SNAPSHOT</version>
</dependency>

<repository>
  <id>org.projectforge.repo</id>
  <name>ProjectForge</name>
  <url>http://www.projectforge.org/nexus/content/repositories/ProjectForge-Snapshots</url>
</repository>
```

## Developers are welcome!
Feel free to fork and we appreciate any pull requests.
