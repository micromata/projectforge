---
title: Reset passwords
categories: [administration]
---

As an administration user you can reset the user's passwords. If you've lost the administrator's password you can reset the password by updating the database entry like `update t_pf_user SET password='SHA{2F1E969683DE272AC96D5AA6033E93A8CB2F283F}' where username='admin';`

This encrypted passwords represents 'manage'. See the log file for encrypted passwords after login failures, if you want to set another password via sql. For HypersonicSQL shutdown the server and edit the ProjectForgeDB.script file like this: `INSERT INTO T_PF_USER VALUES(1, ..., 'SHA{2F1E969683DE272AC96D5AA6033E93A8CB2F283F}',...,'admin')`.