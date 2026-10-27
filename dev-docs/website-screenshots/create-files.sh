#!/usr/bin/env bash
# Creates the sample files (random content, only name and size matter) for upload-files.mjs.
cd "$(dirname "$0")" && mkdir -p files && cd files || exit 1
for f in "Claims-Portal_Specification_v2.3.pdf:2400" "Test-Report_Release-1.4.pdf:860" \
  "Acceptance-Protocol_Sprint-18.pdf:310" "Interface-Description_Claims-API.docx:540" \
  "Effort-Estimate_Document-Archive.xlsx:120" "UI-Mockups_Customer-Area.zip:7800"; do
  head -c $(( ${f##*:} * 1024 )) /dev/urandom > "${f%%:*}"
done
