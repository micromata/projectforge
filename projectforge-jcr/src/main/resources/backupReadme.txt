This zip archive contains all information needed to restore ProjectForge's content repository.

Please note, if modifying this zip archive, that repository.xml/repository.json should be listed in the zip archive
before all attachment filese.
repository.xml/repository.json is used first to create the nodes and properties.

repository.xml
--------------
This file is created by the JackRabbit method Session.exportDocumentView(...). I had some troubles with that:
1. The binaries (if included) will be restored as Base64-encoded. So they were handled by own implemntation after
   restoring nodes.
2. The Session.workspace.importXml(...) doesn't work with sub path.

repository.json
---------------
If you have some troubles while restoring repository, use flag useJson = true to use ProjectForge's own implementation
of restoring the nodes tree including all properties.
