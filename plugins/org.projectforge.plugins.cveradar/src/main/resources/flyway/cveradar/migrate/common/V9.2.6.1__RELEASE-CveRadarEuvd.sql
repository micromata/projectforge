-- CVE-Radar EUVD import (PF-CVR IMP-16): the id of the vulnerability in the EUVD of the ENISA, its EPSS and the day it is
-- known to be exploited since. A vulnerability the EUVD knows only (no CVE id yet) has the EUVD id as its key (cve_id).
ALTER TABLE T_PLUGIN_CVERADAR_VULNERABILITY
    ADD COLUMN euvd_id CHARACTER VARYING(30);
ALTER TABLE T_PLUGIN_CVERADAR_VULNERABILITY
    ADD COLUMN epss NUMERIC(8, 5);
ALTER TABLE T_PLUGIN_CVERADAR_VULNERABILITY
    ADD COLUMN exploited_since TIMESTAMP;
ALTER TABLE T_PLUGIN_CVERADAR_VULNERABILITY
    ADD CONSTRAINT unique_t_plugin_cveradar_vulnerability_euvd_id UNIQUE (euvd_id);

-- An entry of the index derived from the product names of the EUVD (no CPE, no version ranges): matches as possible only.
ALTER TABLE T_PLUGIN_CVERADAR_CPE_MATCH
    ADD COLUMN approximate BOOLEAN DEFAULT FALSE NOT NULL;
