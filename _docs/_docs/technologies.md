---
title: Technologies
subtitle: Enthusiasm for technology as well as a passion for software development resulted in the emergence of a product based on state-of-the-art technologies, which will be improved continuously.
author: kai
tags: [technologies,migration]
---

#### Sections in this article
{:.no_toc}
* TOC
{:toc}

## Supported browsers

ProjectForge is mainly developed under FireFox and Apple Safari. Please refer the following table to see the status of browser interoperability of the current release:

| Browser      | Score | Description                                                                  |
|--------------|-------|------------------------------------------------------------------------------|
| Apple Safari |   ++  | Well tested, works fine.                                                     |
| Chrome       |   ++  | Well tested, works fine.                                                     |
| Firefox      |   ++  | Well tested, works fine.                                                     |
| Microsoft IE |   +   | Quickly tested, seems to work (since ProjectForge's migration to Bootstrap). |


## Professional architecture and CI (continuous integration)

ProjectForge is based on a modern and professional software architecture which is commonly used in theindustry. A professional roles and right management assures that every user can only see and use those data for which he has all the required rights and roles for.
The high quality and reliability of ProjectForge base, last but not least, on a lot of automatically test cases. The more than 250 test cases are running daily, there are part of a professional CI-system (continuous integration). 
A high value of data integrity and security is therefore assured.

## Rapid Application Development Approach (RAD)

ProjectForge is characterized by a rapid application development approach.

This means:

- Write the entity as Java beans and define the data base mapping with annotations (JPA)
- Write simple html pages (Wicket / React)
- Write the business logic in Java/Kotlin (not in jsp or something comparable)
- Derive ui classes (list and edit forms) from the base classes.
- Derive data access objects from the base objects (security, access management, persistancy).

The mission:
Develop entities of new functionalities with lists and edit screens in just a few hours!

- Secure access checking
- Fully indexed (search)
- History of every user's modifications
- Full data base support
- Support of object oriented html pages and components (powered by Wicket)!

An established developer should focus on the essentials!

## Technological requirements for the ProjectForge server

Java 8 might work also with ProjectForge 7, but OpenJDK 11 is recommended (tested in several productive environments), PostgreSQL (optional), Linux, Mac OS X, Windows.

## Technologies

<table>
   <thead>
      <tr>
         <th>Technology</th>
         <th>Usage</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Java/Kotlin</td>
         <td>Base technology</td>
      </tr>
      <tr>
         <td>Ajax</td>
         <td>Used for auto-completion of form input fields and flicker-less browsing. User-specific favorites supports the user for faster access.</td>
      </tr>
      <tr>
         <td>Hibernate / MGC</td>
         <td>Persisting framework O2R, R2O, JPA 2</td>
      </tr>
      <tr>
         <td>Spring</td>
         <td>Boot Application</td>
      </tr>
      <tr>
         <td>Wicket / React</td>
         <td>Web framework Wicket will be replaced by React.</td>
      </tr>
      <tr>
         <td>JCR</td>
         <td>JackRabbit for document storage (used by data transfer tool and others)</td>
      </tr>
      <tr>
         <td>FOP</td>
         <td>For the creation of PDFs, e. g. the user time sheets, monthly employee reports etc.</td>
      </tr>
      <tr>
         <td>JUnit</td>
         <td>Test framework</td>
      </tr>
      <tr>
         <td>Jenkins</td>
         <td>CI - continouos integration system</td>
      </tr>
      <tr>
         <td>Groovy/Kotlin Script</td>
         <td>Used for scripting ProjectForge for reports (MS Excel exports, charting etc.) as well as for e-mail templating (see AdministrationGuide for further information).</td>
      </tr>
      <tr>
         <td>Jasperreports</td>
         <td>For generating reports.</td>
      </tr>
      <tr>
         <td>JFreeChart</td>
         <td>For internal graphics and diagrams as well as for usage in the groovy scriptlets by the users, e. g. for reports.</td>
      </tr>
      <tr>
         <td>Maven3</td>
         <td>Building system, testing, staging and Jar-management.</td>
      </tr>
      <tr>
         <td>Powermock</td>
         <td>For using mock objects in JUnit-tests</td>
      </tr>
      <tr>
         <td>MPXJ</td>
         <td>For exporting MS-Project plans: MPXJ home page</td>
      </tr>
      <tr>
         <td>POI</td>
         <td>For the creation and manipulation of Excel files (also supported in the user scripts).</td>
      </tr>
      <tr>
         <td>SVG (batik)</td>
         <td>For drawing Gantt diagrams</td>
      </tr>
   </tbody>
</table>
