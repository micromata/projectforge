-- The day the lists of libraries of a component were applied the last time (notification about outdated lists).
ALTER TABLE T_PLUGIN_CVERADAR_COMPONENT
    ADD COLUMN libraries_date DATE;
