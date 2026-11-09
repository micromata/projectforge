-- The largest zip of a bulk import of SBOMs into an area, in MB (INV-35); null: as the area above, at the top the
-- configured size.
ALTER TABLE T_PLUGIN_CVERADAR_AREA
    ADD COLUMN sbom_import_max_mb INTEGER;
