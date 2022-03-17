ProjectForge JCR
-----------------

Restoring JackRabbit JCR from backup zip file.

A backup zip archive contains all information needed to restore ProjectForge's content repository (JCR).

Please note, if modifying the backup zip archive, that repository.json should be listed in the zip archive
before all attachment files.

repository.json is used first to create the nodes and properties.

After restoring a sanity check will be done (comparing file sizes and checksums). You may run this sanity check at
any time via click on ProjectForge admin's web page: Administration -> System -> misc checks -> JCR sanity check.


Usage Backup
------------
  java -cp projectforge-application-[version].jar org.projectforge.jcr.BackupMain [jcr-path]
  java -cp projectforge-application-[version].jar org.projectforge.jcr.BackupMain [jcr-path] [backup-dir]

Examples:
  java -cp projectforge-application-[version].jar org.projectforge.jcr.BackupMain /home/kai/ProjectForge/jcr/
  java -cp projectforge-application-[version].jar org.projectforge.jcr.BackupMain /home/kai/ProjectForge/jcr/ /home/kai/backups/


Usage Restore
-------------
  java -cp projectforge-application-[version].jar org.projectforge.jcr.RestoreMain [jcr-path] [backup-zip]

Example:
  java -cp projectforge-application-[version].jar org.projectforge.jcr.RestoreMain /home/kai/ProjectForge/jcr/ projectforge-jcr-backup.zip
