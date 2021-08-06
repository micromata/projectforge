---
title: Quick Start
subtitle: Describes a quick start for new teams, projects etc. and best practices.
author: kai
tags: [setup]
---

#### Sections in this article
{:.no_toc}
* TOC
{:toc}

## Introduction

ProjectForge® is designed to improve the efficiency of projects and project teams from single-person-projects up to large-sized projects. 
This document contains techniques and concepts for your successful project management, supported by ProjectForge®, the project management portal.

You can use the action links while reading this tutorial and create the objects of this tutorial by simply clicking on those action links (in the html version of this tutorial). You can change the tutorial data (user names etc.) like you want. The tutorial only needs the reference names placed in the description fields.
Quick start, create your first project step-by-step: ACME web portal

After the creation of the users you can use the wizard of the structure tree page to generate groups and access rights very quickly. Follow this tutorial to understand the things behind the wizard...

### Set-up of your team members

Login as admin user and create one account per project member (administration sub-menu users). In this example we start with three team members, one project manager and two team members: 

| Username |	Description (team role) | E-Mail                | Assigned groups   |
| -------- | ----------------------- | --------------------- | ----------------- |
| linda    | Project manager         | l.evans@javagurus.com |	PF_ProjectManager |
| dave     | Developer               | d.jones@javagurus.com |	-                 |
| betty    | Developer               | b.brown@javagurus.com | -                 |

### Set-up your company: JavaGurus Inc.

#### Create your company's top level structure element

Login as admin user and create a top level structure element with the name of your company (JavaGurus Inc.). Click on the menu item "structure tree" and on the button "add". Select the ProjectForge®'s root structure element as parent. Create this structure element (and click the search button in the tree view for refreshing your structure tree).

| Name of structure element | Parent structure element               |
| ------------------------- | -------------------------------------- |
| JavaGurus Inc.            |	ProjectForge®'s root structure element | 

### Create a group for all employees

Add a user group (meu item "Administration" -> "Groups") named e. g. "JavaGurus employees".

| Group name          | Assigned members    |
| ------------------- | ------------------- |
| JavaGurus employees | 	Lind, Dave, Betty. |

### Create project member group

Go to administration -> groups and create a new group:

| Group name           | Assigned members    | 
| -------------------- | ------------------- |
| ACME web portal team | Linda, Dave, Betty. | 

Assign the members while selecting all members and clicking the left-arrow-icon. You can select multiple entries by using the keys Shift or Ctrl for assigning them at once.

### Create your project root structure element, e. g. ACME Web portal

Click on the menu entry 'structure tree' and create a structure element named as your project:

| Name of structure element | Parent structure element |
| ------------------------- | ----------------------------------------------------------------------------------------------- |
| ACME web portal           | Root (select parent structure element by clicking on the hierarchy icon and simply select Root). |

can change the hierarchy and the names of your structure elements (e. g. for organizing your projects per customer) later, so start with your project as top level structure element. Also user names, group names etc. can be changed any time you want without loosing any references in the system.

You can change the hierarchy and the names of your structure elements (e. g. for organizing your projects per customer) later, so start with your project as top level structure element. Also user names, group names etc. can be changed any time you want without loosing any references in the system.

 
### Assign the required team access

ProjectForge® has an very detailed access management, so you can configure which user has which access to the system. Enable your ACME web portal team for working with the project. Some access rights depends on the structure elements, so define the access to a structure element by choosing the menu entry 'access management' and clicking the button 'create':

| Structure element | Group                | Recursive |	Description | Access rights |
| ----------------- | -------------------- | --------- | ----------- | ------------- |
| JavaGurus Inc.    | JavaGurus employees  | yes       | The employee's main structure element for books, addresses, for time sheets not assigned to projects such as ill-ness, holiday, research and development etc. | Choose template 'employee'.
| ACME web portal   |	ACME web portal team | yes      | The top-level structure element for the ACME web portal project. | Choose template 'employee'. | 

If they're are any labels or text fields with italic letters (such as the label 'recursive') you'll get a tool-tip explaning the component when you place your mouse over such elements.
Finish

Now your project team members are able to book time sheets on your ACME web portal project. They're also able to give their project a sub structure, explaining in the next chapter: ProjectForge Quickstart Structure of your projects
ProjectForge Quickstart Structure of your projects
The idea of structure trees and hierarchy

ProjectForge® supports a hierarchy of your structure elements without any limitations concerning the depth of your tree or the number of your structure elements. structure element is a general term which can represent customers, projects, project phases, work packages, tasks, issues and further more.
Best practice: recommended structure element structure for your projects

It's recommended to organize your project as followed, see fig. 2 for an example:
Level 	Type 	Description
Top level 	Customer 	e. g. ACME. This is useful for grouping a lot of projects per customers. You can start without this level and can insert it later, if needed.
Level two 	Project name 	Simply the name of your project, e. g. ACME web portal
Level three 	Release 	Your project will have hopefully several releases, so you can group your activities per release. In non-IT projects the release can be something such as e. g. year for annual conferences etc.
Level four 	Phases 	Your project consists of several phases, e. g. akquisition, specification, build, maintenance.
Level five 	Work packages 	For large projects (more than 10 md) it's useful to break down your packages.
Level six 	Issues 	For very large projects it's useful to break down your packages in issues. If you're using the issue tracking system JIRA it's recommended to link the JIRA-Issues to your structure elements and/or time sheets..

Recommended structure hierarchy of a project.

 Please remember: You can change your hierarchy whenever you want, so you can start for example with your project as a top level structure element and insert the customer later, if you'll have more projects and customers.

 

You can add sub structure elements by selecting your the parent structure element and pressing the button 'create structure sub element'. A more convenient method to organize the structure tree of your project is by using the menu entry Gantt.
ProjectForge Quickstart Time sheet booking
Adding time sheets

All team members should book every activity on the project as time sheets. They can book their time sheets in a very convenient way. The easiest way is to use the calendar, click on a day or the start-/stop-times of an existing time-sheet to book new time sheets. 
Every time sheet needs at least a start- and stop time, the structure element on which the user has worked on. Optional the user can enter a JIRA-issue-id for having a direct link to your JIRA-system.

 

Please choose yourself as the user in the calendar view by simply clicking on the smiley at the right top, for displaying all your time sheets you have already booked. 

 
Consumption of work packages

For getting an overview every time you work with ProjectForge® it's recommended to configure your estimated time budgets to every structure element. If the user books his time sheet on a structure element he can instantly see the consumption of this structure element. 
In the structure tree you can see all consumptions of every displayed structure element. 
There are two ways to configure budgets. The easiest way is to enter the budget in hours on every structure element. If a structure element has structure sub elements with given maximum hours and the structure element has itself no budget setting, the budget for this parent structure element is automatically calculated (sum of all structure sub elements budgets). The other way is to define your budgets by assigning orders (described not here). You can mix both approaches.
ProjectForge Quickstart Gantt charts

Planning a project and controlling a project to keep the project on track, Gantt charts are a very often used technique. ProjectForge® supports Gantt charts as descriped in this chapter. 
You can edit all Gantt parameters such as start/stop dates, duration, predecessors etc. by editing the structure element in the structure tree. But it's more convenient to edit the parameters of the structure element in the Gantt tree directly. 
You'll get the Gantt functionality by choosing the menu entry 'Gantt'. Add a new chart and choose your structure element (project, release of project phase) you like to plan. In this example we choose the project's main structure element 'ACME web portal'.

Gantt charts. With the Gantt chart view you can easily create and modify Gantt charts. This view is also recommended, if you want to create your structure tree of your project.
Rules

    Any changes of your Gantt chart will have no effect on the structure elements unless you save the changes to the structure elements (click on green tick to update structure element or decline by clicking the red cross).
    New Gantt objects are marked as titles embraced with asterisks. Unless you don't save this entry as structure element it will not effect your structure tree.
    Rel. means the common 4 Gantt relation types:
        ss

start-start

    sf

start-finish

    default

finish-start

    ff

finish-finish
Export

You're able to export Gantt diagrams to most common graphic formats (PNG, PDF etc.) and to MS Project. The MS Project export considers the holidays configured in ProjectForge®'s config.xml.
ProjectForge Quickstart Cost-unit accounting

Project managers and especially controller like reports, reports and reports. If you like to answer one of the following questions regarding your project(s), you should use the cost features of ProjectForge® as introduced below.

    What is the profit of our project ACME Web portal and of the company ACME over all?
    How many travel costs did we spent for the project ACME Web and for the ACME company at all?
    How many costs did we spent for the warranty for ...?
    How many costs for acquisition for ...
    What was the budget for R&D for our whole company?

Setting up cost objectives, requirements (optional)

Following steps are required to enable your ProjectForge installation for having cost objectives:

    Activating cost

Set the flag "is cost activated" to true (see configuration menu).

    Access rights and required group of the financial administrative user

Please ensure that the financial adminstrative user is assigned to the group PF_FINANCE and the right "cost *" is set to read/write, same for projects.

    Define cost2 types

You've to define cost2 types which are valid for all structure elements and projects. Later you'll be able to do cost calculation based on those cost type, e. g. how much profit do you have with "maintenance" for all projects of customer ACME. Here is an example list to get an idea (feel free to find your own numbers and descriptions, you can extend this list any time later):

    00 - akuisition
    02 - realisation (invoiced)
    03 - testing (invoiced)
    04 - maintenance (invoiced)
    05 - license fees (invoiced)
    06 - specification/documentation (invoiced)
    07 - warranty
    08 - project management (invoiced)
    11 - meetings
    12 - meetings, invoiced (invoiced)
    13 - travelling
    14 - traveling, invoiced (invoiced)
    20 - R&D
    ..

Please note: invoiced is only a flag which you can use later in your reporting scripts.

    Define customers and projects

For project/customer specific cost objectives you'll need to define customers first and then the customer's projects. It's a best practice to have a misc project for every customer to assign time-sheets and cost which aren't specific to a project or for small projects where you don't need a separate cost calculation for.

    Define cost2

You can define cost2 entries (independant from projects) or project specific cost2 entries by selecting the project first.

    Assign projects or cost2 to structure elements

You can assign projects to structure elements in your structure tree (in the project edit page) or by adding non-project-specific cost2 entries directly to a structure element. For project-specific cost-assignments you can use a black/white-list to reduce the available cost2 objects for a sub structure element / or structure sub tree. Please see the structure element edit page for a better understanding.

### ProjectForge Quickstart Calendars

Please refer the user guide for further information.

### ProjectForge Quickstart Best practices

Fast and mouse-less editing - default buttons

In almost all dialogs the change button is the default submit button (green button). This button is used automatically if the user hits the RETURN key inside form fields. In Text-areas you may use the CTRL-RETURN key because the normal return key produces a new line.

#### Making your life easier: with Favorites

As a project manager you need often to select users, so have a look at the select box right to each user selection panel. Select a user and then choose 'create' for generating a favorite entry for users. 
It's also possible to create favorites for structure elements, time sheets (templates) etc. 
You can manage your favorites by choosing the menu entry 'my preferences'.

#### Sharing addresses and books

Go to "Administration" -> "Settings" and configure the top level structure element of your company (JavaGurus Inc.) as default value for addresses and books. Afterwards all your employees are able to work with your library and company address book.

#### Administrators, financial administrative user, organisational staff members and controlling users

In larger companies it's use-full to split this role.

#### System administrators

You should have system administrators who're responsible to manage users, groups and access rights to structure elements and functionalities.

#### Financial administrative users

You should have users of your financial administrative staff who're responsible to manage employees (and salaries), customers, order book, cost objectives, projects as well as invoices (outbound and inbound). You can specify which rights each user should have. The user's of the group "PF_Organization" (if exist) have reduced rights at default.

#### Project managers

Project managers are able to plan human resources and to organize the order book for the projects they're members of the project manager group (set in project details).

#### Controllers

Members of the group "PF_Controlling" have read-only access to most areas of ProjectForge at default. The also have the possibility to use, create and modify scripts. The scripts are run with read-only access. The rights of the user who runs a script is checked for every object read of the data-base. If the user has no read-access to one object, the object is removed from the result-set. (In future releases scripting will be accessible by all user with the "scripting"-right). ProjectForge ensures, that a scripting user will always have access to only those objects he has the read-access for. But you should only give this access to well-known users, because a fraud has the possibility to get write access via e. g. Java reflection API.
