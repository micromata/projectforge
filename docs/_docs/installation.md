---
title: Installation
subtitle: Easy and fast installation with an new setup wizard (test versions as well as productive setups)
author: kai
tags: [setup]
---

#### Sections in this article
{:.no_toc}
* TOC
{:toc}

# Express installation

You may choose now between Docker based installation or normal
installation.

## Docker

You may install ProjectForge as docker image.

We assume `/home/projectforge/ProjectForge` as ProjectForge’s home in
this documentation, but you may choose anything else (will be created or
should be empty if already exists).

For test or small installations you may use the built-in data base
(embedded).

### Running single docker container

1.  First start
    `docker run -t -i -p 127.0.0.1:8080:8080 -v $HOME/ProjectForge:/ProjectForge --name projectforge micromata/projectforge`

2.  Follow setup wizard below.

3.  After finalization of setup page in browser, press `CTRL-C` for
    stopping container.

4.  Restart `docker start projectforge`

5.  Enjoy.

You may monitor the log file: `tail -f ~/ProjectForge/logs/ProjectForge.log`

### Running as stack (docker-compose)

In this docker-compose case ProjectForge is composed with a PostgreSQL
docker container (for productive and larger installations).

1.  Copy and modify
    [docker-compose.yml](https://github.com/micromata/projectforge/blob/master/docker/compose/docker-compose.yml)
    (change data base password).

2.  First start: `docker-compose run projectforge-app`

3.  Follow setup wizard below (choose PostgreSQL connection settings).

4.  ProjectForge stops automatically after setting up your ProjectForge
    directory. Restart `docker-compose up`

5.  After finalization of setup page in browser, press `CTRL-C` or
    `docker-compose down` for stopping container.

6.  Restart `docker-compose up`

7.  Enjoy.

You may monitor the log file: `tail -f ~/ProjectForge/logs/ProjectForge.log`

### Building docker container from source

1.  `git clone git@github.com:micromata/projectforge.git`

2.  `cd projectforge`

3.  `docker build -t micromata/projectforge:latest .`

### Increasing memory

Edit `~/ProjectForge/environment.sh` for customizing settings of
ProjectForge as environment variables.

## Java-Installation (without docker)

If you don’t want to use Docker, you’re able to install ProjectForge
from the executable jar file.

1. Install openjdk (java 8 should also work, if needed, 11+
    recommended).

2. Get projectForge-application\_version.jar

3. First start: java -jar projectForge-application\_version.jar

Here you will find the supported versions of Java:
[???](#Supported Java versions)

## The installation setup wizard (for all installations)

After first start, you’ll be asked for entering the setup wizard. Go for
it. There is an console setup wizard available as well as a graphical
wizard as desktop application, depending on your environment. You may
choose the version if your system supports both (graphical output and
terminal output).

Both editions have the same functionality.

{% include image.html img="setup-wizard-step-1.png" alt="Setup wizard" caption="The setup wizard (terminal edition) for choosing ProjectForge’s home
directory. This step is skipped on docker based installation." %}

We assume `/home/projectforge/ProjectForge` as ProjectForge’s home in
this documentation, but you may choose anything else (will be created or
should be empty if already exists).

{% include image.html img="setup-wizard-step-2.png" alt="Setup wizard (terminal edition)" caption="The setup wizard (terminal edition) for configuring the basic settings." %}

{% include image.html img="setup-wizard-gui-jdbc.png" alt="Setup wizard (graphical edition)" caption="The setup wizard (graphical edition) for configuring and testing the data base connection." %}

You may leave the most settings as they are. You are able to change
these settings later in `projectforge.properties` or `config.xml`.

<table>
<colgroup>
<col style="width: 25%" />
<col style="width: 75%" />
</colgroup>
<tbody>
<tr>
<td style="text-align: left;"><strong>Directory</strong></td>
<td style="text-align: left;">ProjectForge’s home directory including configuration, database and working directory.</td>
</tr>
<tr>
<td style="text-align: left;"><strong>Domain</strong></td>
<td style="text-align: left;">The domain of your system (<a href="http://localhost:8080">http://localhost:8080</a> for test systems). This is needed e. g. for sending e-mail-notification to users including direct links to your installation of ProjectForge.</td>
</tr>
<tr>
<td style="text-align: left;"><strong>Port</strong></td>
<td style="text-align: left;">ProjectForge starts the server on this port (8080 should be OK for most cases and can’t be modified for docker installation).</td>
</tr>
<tr>
<td style="text-align: left;"><strong>Database</strong></td>
<td style="text-align: left;">Choose the data base. Embedded uses the built-in data base of ProjectForge (Hsql DB) and should be OK for test, development or small instances. In docker mode only PostgreSQL is available.</td>
</tr>
<tr>
<td style="text-align: left;"><strong>Jdbc settings</strong></td>
<td style="text-align: left;">If you choose PostgreSQL you are able to enter the data base connection values and test them by clicking <strong>Test connection</strong>.</td>
</tr>
<tr>
<td style="text-align: left;"><strong>Currency</strong></td>
<td style="text-align: left;">The default currency to use.</td>
</tr>
<tr>
<td style="text-align: left;"><strong>Locale</strong></td>
<td style="text-align: left;">The default locale to use. Your users are able to choose their own language.</td>
</tr>
<tr>
<td style="text-align: left;"><strong>First day</strong></td>
<td style="text-align: left;">The first day of week to use in the calendar views.</td>
</tr>
<tr>
<td style="text-align: left;"><strong>Time</strong></td>
<td style="text-align: left;">The default time notation to use (customizable by the users).</td>
</tr>
<tr>
<td style="text-align: left;"><strong>Setting</strong></td>
<td style="text-align: left;">Start ProjectForge - If checked, ProjectForge will be started after clicking <strong>Finish</strong>. For embedded data base, the data base is created.</td>
</tr>
<tr>
<td style="text-align: left;"><strong>Setting</strong></td>
<td style="text-align: left;">Enable CORS filter - Please check only for development (React development using yarn or npm). Do not use for productive systems!!!</td>
</tr>
</tbody>
</table>

After clicking finish, ProjectForge will be initialized and started. You
may proceed with your web browser with `http://localhost:8080` or
`https://projectforge.acme.com`.

If your browser doesn’t support `http://localhost:8080`, try
*http://127.0.0.1:8080/* or *http://127.0.0.1:8080/wa/setup* or another
browser.

ProjectForge is only available on port 8080 from localhost due to
security reasons. For using https, please refer
[???](#Reverse Proxy Setup (https)).

## The setup page

Please be aware, that after your first start of ProjectForge, your page
might be public and be configured by anybody else! So proceed
immediatelyly with the configuration if your new ProjectForge instance
is public available.

{% include image.html img="setup-webpage.png" alt="Setup Webpage" caption="After starting ProjectForge the first time, a setup page is displayed." %}

<table>
<colgroup>
<col style="width: 25%" />
<col style="width: 75%" />
</colgroup>
<tbody>
<tr>
<td style="text-align: left;"><strong>Target</strong></td>
<td style="text-align: left;">Choose <strong>Productive system</strong> for starting with an empty initialized data base. Choose <strong>Test system</strong> for installing a test system with lots of test data.</td>
</tr>
<tr>
<td style="text-align: left;"><strong>User name</strong></td>
<td style="text-align: left;">The user name of the initial admin user of ProjectForge.</td>
</tr>
<tr>
<td style="text-align: left;"><strong>Password</strong></td>
<td style="text-align: left;">Admin’s password.</td>
</tr>
<tr>
<td style="text-align: left;"><strong>Default time zone</strong></td>
<td style="text-align: left;">Default time zone for all users, if not configured by an user und MyAccount.</td>
</tr>
<tr>
<td style="text-align: left;"><strong>Calendar domain</strong></td>
<td style="text-align: left;">ProjectForge provides calendar and events. For having world-wide unique event id’s, choose here your personal name.</td>
</tr>
<tr>
<td style="text-align: left;"><strong>Administrators</strong></td>
<td style="text-align: left;">ProjectForge sends e-mails to this address(es) in the case of special errors. You can specify one ore more (coma separated) addresses (RFC822).</td>
</tr>
<tr>
<td style="text-align: left;"><strong>Feed-back</strong></td>
<td style="text-align: left;">If this e-mail is given then a feedback panel will be shown if an error occurs. The user has the possibility to send an e-mail feedback (e. g. JIRA-system or help desk).</td>
</tr>
</tbody>
</table>

Just click finish to have your ready-to-use installation.

{% include image.html img="setup-webpage-finished.png" alt="Setup Webpage finished" caption="After initialization you will get this screen. Now restart a last time and also all activated plugins are now fully available." %}

Wait until ProjectForge’s initialization is finished and you are
requested to restart ProjectForge. After restarting all activated
plugins are also available.

## Activation of built-in-plugins

![You have to activate some built-in plugins if you want to use them.
The plugin "Data transfer" is recommended.](images/admin-plugins.png)

# Customization

## Main configuration file `projectforge.properties`

You’ll find an overview of all configuration options here:
[application.properties](https://github.com/micromata/projectforge/blob/master/projectforge-business/src/main/resources/application.properties)

A minimal set of `projectforge.properties` will be installed
automatically by the setup wizard.

Here you may define your company logo.

## Configuration parameters

You’ll find further configuration params through the web application
under the menu *Administration* → *Configuration*.

## Special configurations, file `config.xml`

A minimal set of `config.xml` will be installed automatically by the
setup wizard. Here you may define your specific holidays.

# Secure http connection (SSL/https)

The recommended way of setting up ProjectForge is to use a reverse proxy
to do the SSL termination.

There are different ways to do so.

## Using built-in functionality

Without nginx, Apache etc. you may use the ProjectForge’s built-in
functionalities, see e. g.
<https://www.baeldung.com/spring-boot-https-self-signed-certificate>

## Nginx

### Prepare

All of the commands below should be run with `root` privileges.

1.  Install Nginx: `$ apt-get install nginx`

2.  Get an SSL certificate(use only one of the options below)

    1.  Create self signed certificate:
        `$ openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout /etc/ssl/projectforge.key -out /etc/ssl/projectforge.crt`

    2.  Generate an SSL certificate [using
        Letsencrypt](https://letsencrypt.org/getting-started/), note
        that the path in the NGINX configuration below needs to be
        changed when using Letsencrypt.

3.  Generate secure Diffie-Hellman parameters for key exchange (this
    will take a long time):
    `$ openssl dhparam -out /etc/nginx/dhparam.pem 4096`

#### secure domain for setup through htpasswd (optional)

1.  `$ apt-get install apache2-utils`

2.  `$ htpasswd -c /etc/nginx/.htpasswd projectforge`

### Configure Nginx

To use NGINX as a reverse proxy, it’s necessary to create a
configuration file. The standard path for NGINX configurations is
`/etc/nginx/sites-available/`, so let’s create the file
[`/etc/nginx/sites-available/projectforge`](misc/nginx_sites-available_projectforge).
If you want to use `.htaccess` to blok access to the installation, you
need to remove the comment character (`#`) in front of the `auth_basic`
and `auth_basic_file` parameters. If you want to use HSTS (which makes
browsers show an error page when the SSL certificate is invalid and/or
nonexistent), remove the comment character (`#`) in front of the
`add_header Strict-Transport-Security` parameter.

**Remeber to replace **projectforge.example.com** with the actual domain
you’ll run ProjectForge on!**

To activate the NGINX configuration, you’ll need to symlink the
configuration file we just created to `/etc/nginx/sites-enabled`. This
can be done by using the following command:

    $ ln -sv /etc/nginx/sites-available/projectforge /etc/nginx/sites-enabled/projectforge

## Apache httpd

to be defined.

# Using CardDAV and WebDAV with Milton

Place files `milton.license.properties` and `milton.license.sig` to
directory `~/ProjectForge/resources/milton/` and start ProjectForge with
loader path:

    ${JAVA} ... -Dloader.path=${HOME}/ProjectForge/resources/milton ${DEBUGOPTS} -jar projectforge-application.jar &

# Start ProjectForge (without docker)

1.  Start ProjectForge server (e.g. on `http://localhost:8080`, update
    the NGINX config if you use another port).

2.  Follow the configuration instruction (setup wizard in console ui or
    as Desktop app).
a
3.  (Re-)start Nginx: 3.1. SysVInit: `/etc/init.d/nginx restart` 3.2.
    SystemD: `systemctl restart nginx`

4.  Navigate to ProjectForge with your browser and finalize the setup.

    -   Example start script:
        [startProjectForge.sh](misc/startProjectForge.sh)

    -   Example stop script:
        [stopProjectForge.sh](misc/stopProjectForge.sh)

# Adding external plugins

ProjectForge supports external 3rd party plugins: 1. Place your jars e.
g. in `/home/kai/ProjectForge/plugins` 2. Tell ProjectForge where it is.
You may have to options: a. Run ProjectForge from command line with
option `-Dloader.home=/home/kai/ProjectForge`, or b. Set the environment
variable before starting ProjectForge:
`export LOADER_HOME=/home/kai/ProjectForge`. 3. Start ProjectForge and
activate the plugin as admin in the ProjectForge’s web app under menu
Admin→plugins. 4. Restart ProjectForge.

# Backups

## JCR

Attachments will be handled through the built-in JCR module. The backups
are placed in `ProjectForge/backup`, the daily backups will purged after
30 days keeping each first monthly backup.

## DB backup

You may configure a purge job in `projectforge.properties`:

    ### If purgeBackupDir is given and exists, ProjectForge will purge daily backups older than 30 days keeping each first monthly backup.
    ### The filenames must contain the date in ISO format (...yyyy-MM-dd....).
    # This is the backup dir to look for:
    projectforge.cron.purgeBackupDir=/home/projectforge/backup
    # You may optional specify the prefix of the backup files (if not given, all files containing a date in its filename will be processed):
    projectforge.cron.purgeBackupFilesPrefix=projectforge_

Your daily data base backups should contain the date of backup in ISO
format in its file name. Daily backups (not monthly) will be deleted
after 30 days. Refer config file for all options:
\[<https://github.com/micromata/projectforge/blob/develop/projectforge-business/src/main/resources/application.properties>\]

# Supported Java versions

<table>
<caption>Java Compability (2021/04/12)</caption>
<colgroup>
<col style="width: 14%" />
<col style="width: 14%" />
<col style="width: 14%" />
<col style="width: 14%" />
<col style="width: 42%" />
</colgroup>
<tbody>
<tr>
<td style="text-align: left;">Java version</td>
<td style="text-align: left;">V6*</td>
<td style="text-align: left;">V7.0*</td>
<td style="text-align: left;">V7.1+</td>
<td style="text-align: left;">Comments</td>
</tr>
<tr>
<td style="text-align: left;">Oracle 1.8</td>
<td style="text-align: left;">+++</td>
<td style="text-align: left;">+</td>
<td style="text-align: left;">+</td>
<td style="text-align: left;">Oracle 1.8 was in production for years up to version 7.1.2. (LDAP master/slave needs Java 11 since 7.1*)</td>
</tr>
<tr>
<td style="text-align: left;">OpenJDK 11</td>
<td style="text-align: left;">-</td>
<td style="text-align: left;">+++</td>
<td style="text-align: left;">+++</td>
<td style="text-align: left;">OpenJDK 11 is used in development (MacOS) and in heavy production since version 7.0.0. (Linux)</td>
</tr>
<tr>
<td style="text-align: left;">OpenJDK 12</td>
<td style="text-align: left;">-</td>
<td style="text-align: left;">+</td>
<td style="text-align: left;">?</td>
<td style="text-align: left;">Should run since version V7.0</td>
</tr>
<tr>
<td style="text-align: left;">OpenJDK 13</td>
<td style="text-align: left;">-</td>
<td style="text-align: left;">+</td>
<td style="text-align: left;">?</td>
<td style="text-align: left;">Should run since version V7.0</td>
</tr>
<tr>
<td style="text-align: left;">OpenJDK 14</td>
<td style="text-align: left;">-</td>
<td style="text-align: left;">-</td>
<td style="text-align: left;">?</td>
<td style="text-align: left;">Should run since version V7.1</td>
</tr>
<tr>
<td style="text-align: left;">OpenJDK 15</td>
<td style="text-align: left;">-</td>
<td style="text-align: left;">-</td>
<td style="text-align: left;">+</td>
<td style="text-align: left;">Should run since version V7.1</td>
</tr>
<tr>
<td style="text-align: left;">OpenJDK 16</td>
<td style="text-align: left;">-</td>
<td style="text-align: left;">-</td>
<td style="text-align: left;">+</td>
<td style="text-align: left;">Should run since version V7.1</td>
</tr>
</tbody>
</table>

<table>
<caption>Legend</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<tbody>
<tr>
<td style="text-align: left;">-</td>
<td style="text-align: left;">?</td>
<td style="text-align: left;">+</td>
<td style="text-align: left;">+++</td>
</tr>
<tr>
<td style="text-align: left;">doesn’t run</td>
<td style="text-align: left;">not tested</td>
<td style="text-align: left;">should run, shortly tested</td>
<td style="text-align: left;">recommended, tested in production</td>
</tr>
</tbody>
</table>

