ProjectForge JCR
-----------------

Restoring JackRabbit JCR from backup zip file.

A backup zip archive contains all information needed to restore ProjectForge's content repository (JCR).

Please note, if modifying the backup zip archive, that repository.json should be listed in the zip archive
before all attachment files.

repository.json is used first to create the nodes and properties.

After restoring a sanity check will be done (comparing file sizes and checksums). You may run this sanity check at
any time via click on ProjectForge admin's web page: Administration -> System -> misc checks -> JCR sanity check.

Prepare application
-------------------
You can't run the main classes directly from ProjectForge's jar file. Following steps are needed to prepare the application:
1. Extract the ProjectForge application jar file.
   mkdir extracted
   cd extracted
   jar xf ../projectforge-application-<version>.jar
2. Set CLASSPATH to the extracted directory.
   CLASSPATH="BOOT-INF/classes:$(find BOOT-INF/lib -name '*.jar' | tr '\n' ':')"
3. Run the main classes from the extracted directory.
   java -cp "$CLASSPATH" org.projectforge.jcr.BackupMain

Usage Backup
------------
  # ./ is used for creating zip archive of backup:
  java -cp "$CLASSPATH" org.projectforge.jcr.BackupMain [jcr-path]
  # [backup-dir] is used for creating zip archive of backup:
  java -cp "$CLASSPATH" org.projectforge.jcr.BackupMain [jcr-path] [backup-dir]

Examples:
  java -cp "$CLASSPATH" org.projectforge.jcr.BackupMain /home/kai/ProjectForge/jcr/
  java -cp "$CLASSPATH" org.projectforge.jcr.BackupMain /home/kai/ProjectForge/jcr/ /home/kai/backups/


Usage Restore
-------------
  mv [jcr-path] [jcr-path].bak # Move original jcr-path to jcr-path.bak
  mkdir [jcr-path]
  java -cp "$CLASSPATH" org.projectforge.jcr.RestoreMain [jcr-path] [backup-zip]

Example:
  java -cp "$CLASSPATH" org.projectforge.jcr.RestoreMain /home/kai/ProjectForge/jcr/ projectforge-jcr-backup.zip

Check file integrity
--------------------
  java -cp "$CLASSPATH" org.projectforge.jcr.SanityCheckMain [jcr-path]

Corrupted segments
------------------
Try to do a backup as described above. If the backup fails, you may have corrupted segments in your repository.

You may also detect corrupted segments, if you see the following error message in the log file:
ERROR org.apache.jackrabbit.oak.segment.SegmentNotFoundExceptionListener -- Segment not found: ...

If you have corrupted segments, execute a backup and restore as described above.
