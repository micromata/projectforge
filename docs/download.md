---
layout: page
width: small
title: Download
permalink: /download/
---

## Community Edition

<p class="uk-text-lead">This community edition is full-featured and for free, there are no limitations!</p>

An auto-update mechanism for the data-base on start-up of the web app is included for all previous public versions: Convenientupdates.

<table>
  <thead>
    <tr>
      <th>Type</th>
      <th>Link</th>
      <th>Size</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Windows / MacOS X / Other</td>
      <td><a href="https://sourceforge.net/projects/pforge/files/ProjectForge/" target="_blank">ProjectForge-application-X.X.X.jar</a></td>
      <td>130MB</td>
      <td>Executable java archive</td>
    </tr>
    <tr>
      <td>Docker hub</td>
      <td><a href="https://hub.docker.com/repository/docker/kreinhard/projectforge" target="_blank">Docker images</a></td>
      <td>530MB</td>
      <td>Docker images for running as single container as well as docker compose stack.</td>
    </tr>
    <tr>
      <td>Sources</td>
      <td><a href="https://github.com/micromata/projectforge" target="_blank">Sources on Github</a></td>
      <td>-</td>
      <td>All sources for own development.</td>
    </tr>
    <tr>
      <td></td>
    </tr>
  </tbody>
</table>

### Source code at GitHub

You can also check out the source code from [GitHub](https://github.com/micromata/projectforge)
 
## Convenient updates

<p class="uk-text-lead">Updates through simple clicks</p>

ProjectForge provides a very convenient method for updating your ProjectForge installation. Since version 6 any required updates or migrations will be done automatically by simply starting a new version.

Some versions need manual updates (e. g. of config files): [Migration](https://github.com/micromata/projectforge/blob/develop/doc/migration.adoc).

If you need to update versions older 6, you'll need to download the latest version 5 and use the older update mechanism by simply clicking through the updates.


## Translation files

<p class="uk-text-lead">ProjectForge is 100% internationalized. Enable new languages by simply editing the translation file.</p>

Currently ProjectForge is available in the languages English and German. For enabling a new language you only need to enter your translations in the translation text file. Please choose one of the existing language files which you will use as a template for your translations. Please leave the i18n key untouched and change the translation right to the '=' character.
This is the format of a translation file:

```
# <ProjectForge key>=<Translation>
address.addresses=Addresses
address.addressStatus=Address status
address.addressStatus.leaved=company leaved
address.addressStatus.outdated=out-dated
address.addressStatus.uptodate=up-to-date
...
menu.accessList=Access management
menu.addNewEntry=New entry
menu.addressList=Addresses
menu.administration=Administration
...
task.assignedUser=Assigned user
task.consumption=Consumption
task.deleted=deleted
```

The translations are grouped, it's possible to translate only parts of the file. If any entry is missing in the user's language the translation from the default language is used (English).
Use one of the following translation files as template:

- [I18nResources.properties](https://github.com/micromata/projectforge/blob/master/projectforge-business/src/main/resources/I18nResources.properties}: The English (default) translation file.
- [I18nResources_de.properties](https://github.com/micromata/projectforge/blob/master/projectforge-business/src/main/resources/I18nResources_de.properties): The German translation file.

Put the translation file to the path:`src/main/resources`.
Please send any translation file to k.reinhard at me.com, so it'll be part of the next distribution.
For any newer version of ProjectForge you'll get a list of new translations not yet available in your language (please refer the system administration menu).

## Archive

<table>
  <thead>
    <tr>
      <th>Date</th>
      <th>Link</th>
      <th>Size</th>
      <th>Platform</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>2016-08-16</td>
      <td>Version history on sourceforge: <a target="_blank" href="https://sourceforge.net/projects/pforge/files/ProjectForge/">ProjectForge on SourceForge</a></td>
      <td>&nbsp;</td>
      <td>&nbsp;</td>
    </tr>
    <tr>
      <td>2016-07-18</td>
      <td><a target="_blank" href="https://sourceforge.net/projects/pforge/files/ProjectForge/6.1/ProjectForge-application-6.1.0.zip/">ProjectForge-application-6.1.0.zip</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2016-04-27</td>
      <td><a target="_blank" href="http://downloads.sourceforge.net/project/pforge/ProjectForge/6.0/ProjectForge-6.0.zip/">ProjectForge-6.0.zip</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2014-05-16</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/5.4/">ProjectForge-5.4.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2013-12-30</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/5.3/">ProjectForge-5.3.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2013-07-07</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/5.2/">ProjectForge-5.2.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2013-05-14</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/5.1/">ProjectForge-5.1.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2013-04-10</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/5.0/">ProjectForge-5.0.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2013-02-06</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/4.3.1/">ProjectForge-4.3.1.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2013-01-26</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/4.3/">ProjectForge-4.3.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2012-12-04</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/4.2/">ProjectForge-4.2.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2012-06-16</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/4.1.3/">ProjectForge-4.1.3.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2012-05-03</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/4.1.0/">ProjectForge-4.1.0.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2012-04-18</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/4.0.0/">ProjectForge-4.0.0.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2011-05-27</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/3.6.1/">ProjectForge-3.6.1.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2011-03-18</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/3.6.0/">ProjectForge-3.6.0.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2011-02-24</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/3.5.4/">ProjectForge-3.5.4.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2011-02-14</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/3.5.3/">ProjectForge-3.5.3.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2011-02-03</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/3.5.2/">ProjectForge-3.5.2.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2011-01-26</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/3.5.1/">ProjectForge-3.5.1.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2011-01-23</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/3.5.0/">ProjectForge-3.5.0.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2010-11-17</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/3.4.3">ProjectForge-3.4.3.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2010-11-08</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/3.4.2">ProjectForge-3.4.2.*</a></td>
      <td>-</td>
      <td>all</td>
    </tr>
    <tr>
      <td>2010-10-04</td>
      <td><a target="_blank" href="http://sourceforge.net/projects/pforge/files/ProjectForge/3.4.1">ProjectForge-3.4.1.*</a></td>
      <td>-</td>
      <td>web-server</td>
    </tr>
  </tbody>
</table>
