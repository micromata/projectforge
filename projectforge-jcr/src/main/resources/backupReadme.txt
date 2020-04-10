ProjectForge JCR
-----------------

Restoring JackRabbit JCR from backup zip file.

A backup zip archive contains all information needed to restore ProjectForge's content repository (JCR).

Please note, if modifying the backup zip archive, that repository.json should be listed in the zip archive
before all attachment files.

repository.json is used first to create the nodes and properties.


Usage
-----
  java -cp projectforge-application-[version].jar org.projectforge.jcr.MainKt [backup-zip] [jcr-path]

Example:
  java -cp projectforge-application-[version].jar org.projectforge.jcr.MainKt projectforge-jcr-backup.zip /home/kai/ProjectForge/jcr/
