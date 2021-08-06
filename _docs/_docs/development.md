---
title: ProjectForge development documentation
author: kai
tags: [setup]
---

#### Sections in this article
{:.no_toc}
* TOC
{:toc}


# Want to see it live?

You’re welcome to run ProjectForge by simply doing:

1.  Download the current version 7 snapshot (beta version):
    [projectforge-application-7.0-SNAPSHOT.jar](https://sourceforge.net/projects/pforge/files/ProjectForge/Snapshots/projectforge-application-7.0-SNAPSHOT.jar/download)

2.  Run `java -jar projectforge-application-7.0-SNAPSHOT.jar` (Java 8 is
    needed).

3.  Visit <http://localhost:8080/wa/setup>

4.  Choose and enter a password for the admin and submit.

5.  ProjectForge is now creating a system with some test data in a few
    seconds.

6.  Enjoy it and test the REST API.

# Architecture

## The layers

The main functionality of ProjectForge is to display and search data
list and to modify data (CRUD - create, read, update and delete).
Therefore the goal of the architecture is not to write thousands lines
of code for same base functionality.

### Business layer (business)

The business layer handles all entities including access checking for
CRUD operations as well as well as user services (storage for
preferences etc.).

### REST API and front-end support (rest)

The REST layer provides CRUD services etc. and also front-end support
functionality. These both parts might be separated, but regarding code
efficiency both are combined. For using only CRUD operations (without
interest in the front-end support functionality), so simply ignore some
additional information in the result data (json).

Yes, you’re totally right: Should you not separate front-end from
back-end code more strictly? But, the code is separated and despite the
fact, that UI layout info and CRUD services are combined, it should be
allowed to have a server-side application support. So this REST-API is a
result of efficient development without dozens of front-end and back-end
developers.

### ReactJS (webapp)

The ReactJS layer handles the data and UI layout information provided by
the REST API. A dynamic layout management system is used to visualize
the html pages defined by the server. In addition customized components
are provided for specialized functionality.

Except for the customized components and pages, the client has no clue,
of what entities are provided by the server. The client gets the menu
from the server and calls the server for how to proceed for given
entities (e. g. list or edit view of users, addresses, book, time sheets
etc.). The server provides both, the data to display (model) and the
layout information of how to display (view). The users actions (clicking
on buttons or menu entries) results in most cases in server calls, and
the server responses how to proceed:

1.  The front-end supports defined state manipulations by the server
    (see below, e. g. selecting favorite filter etc., or list of
    calendar events after selecting calendars etc.)

2.  The server may respond in addition on URLs to redirect to by the
    front-end.

3.  The server provides also validation information (e. g. max-length of
    text input fields) as well as errors after complex server-side
    validations after submitting a form.

4.  The front-end defines customized pages and components (non standard
    components are not ruled by the server). Examples: Image upload for
    addresses or lend-out functionality for books.

# Backend

Java is the language used since the very beginning of ProjectForge in
2001.

With ProjectForge 7.x Kotlin and ReactJS is introduced.

## Why Kotlin in addition to Java?

-   Kotlin is used for smarter and less code, especially for BaseDO
    objects as well as for easier creation of data transfer classes for
    the new introduced REST API.

-   Other advantages:

    -   Smart operations on collections (map, find, sort etc.)

    -   Immutable objects, classes and functions as default.

    -   Null safety

    -   Compability with JAVA code.

-   Java and Kotlin will both exist. Only most BaseDO classes were
    migrated and new functionality will mostly be written in Kotlin.

Please note: Kotlin must be compiled with target 1.8 (Java version) but
is fully runnable on Java 9, 10 and 11 VMs.

### Base entities (BaseDO)

The BaseDO objects are the entities of ProjectForge stored in the data
base. As default, all modification of entity attributes are available in
a history with the information about the modification (user, timestamp,
old and new value).

**AddressDO.kt**

    open class AddressDO : DefaultBaseWithAttrDO<AddressDO>() {

        @PropertyInfo(i18nKey = "address.contactStatus")
        @Field
        open var contactStatus = ContactStatus.ACTIVE

        @PropertyInfo(i18nKey = "name", required = true)
        @Field
        @get:Column(length = 255)
        open var name: String? = null

        @PropertyInfo(i18nKey = "address.phone", additionalI18nKey = "address.business")
        @Field
        @get:Column(length = 255)
        open var businessPhone: String? = null
        ...
    }

BaseDO classes must be declared as open as well as all properties.
Otherwise JPA/Hibernate isn’t able to proxy these objects and lazy
loading isn’t supported.

As example some parts of `AddressDO.kt` are shown and described below:

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p><code>@PropertyInfo</code></p></td>
<td style="text-align: left;"><p>The given <code>i18nKey</code> is used for translating the field label and will be served for the frontend(s). The optional given <code>additionalI18nKey</code> is used for having an additional translated label, in the example there are different phone numbers, categorized as business or private.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>@Field</code></p></td>
<td style="text-align: left;"><p>This database field will be indexed and available for a full text search as well as for specifying search values for this field by the user.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>@get:Column(length=255)</code></p></td>
<td style="text-align: left;"><p>JPA annotations. The JPA annotations are available as Meta information from all parts and will be served for the frontends, e. g. for defining the html field <code>max-length</code> of input fields.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Property type</p></td>
<td style="text-align: left;"><p>The property type is also available as Meta information also for the clients. The input fields of the frontend may be autodetected (string, date picker, user selectors, drop down choices for enums etc.)</p></td>
</tr>
</tbody>
</table>

**ContactStatus.java**

    public enum ContactStatus implements I18nEnum
    {
      ACTIVE("active"), NON_ACTIVE("nonActive"), DEPARTED("departed");

      public String getI18nKey()
      {
        return "address.contactStatus." + key;
      }
      ...
    }

The enumerations of type `I18nEnum` are also designed for auto
translation purposes. The field `contactStatus` will be presented as a
drop down choice field with translated labels.

### BaseDao

The BaseDao classes provide all CRUD operations for the BaseDO entities
and will handle the access rights. No user is able to select or modify
entities without the required access rights.

The implementation of BaseDao for entities, such as users, addresses,
books etc. extends the BaseDao object by defining the access rights and
additional special functionality. The base CRUD functionality including
access checks, history service etc. will be inherited.

# REST API

Since version 7.0 ProjectForge provides all CRUD operations through a
REST API and much more. The user’s access rights will be checked. For
available standard REST calls you may refer the REST calls described in
the UI section below.

# UI

The new UI is based on REST and ReactJS. The ReactJS code includes a
dynamic auto layout component for standard components, such as:

For developing ProjectForge’s frontend, please refer:
<https://github.com/micromata/projectforge/tree/develop/projectforge-webapp>

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Input</p></td>
<td style="text-align: left;"><p>Html input fields (text, date picker with text input etc.)</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Select boxes</p></td>
<td style="text-align: left;"><p>For selecting values for e. g. enums (auto completion and asynchronous are calls supported.)</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Multi select</p></td>
<td style="text-align: left;"><p>Select field for selecting multi values (auto completion, asynchronous). This may be used for selecting values as well as of selecting entities assigned to current object, e. g. users may assigned to groups or calendars are selectable for displaying.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Fieldset</p></td>
<td style="text-align: left;"><p>Fieldsets with titles and length settings (Bootstrap grid system is supported)</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Columns</p></td>
<td style="text-align: left;"><p>Columns with length settings (Bootstrap grid system is supported)</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Tables</p></td>
<td style="text-align: left;"><p>For displaying result sets etc.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Customized fields</p></td>
<td style="text-align: left;"><p>You may register customized UI components which will be used for displaying and modifiing values. Refer the image upload for addresses as an example.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>…</p></td>
<td style="text-align: left;"><p>…</p></td>
</tr>
</tbody>
</table>

## Responsive

Bootstrap is used and responsive layout control is fully supported.

## Standard list views

Available REST calls:

<table>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Rest call</p></td>
<td style="text-align: left;"><p>Description</p></td>
<td style="text-align: left;"><p>Return values</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>rs/address/initialList</code></p></td>
<td style="text-align: left;"><p>Initial call for displaying a list including layout, recent filter settings, result data and favorites.</p></td>
<td style="text-align: left;"><ul>
<li><p>UI layout (available filter options, columns of the result data, page menu items, …)</p></li>
<li><p>Recent used filter settings by the user.</p></li>
<li><p>Available personal favorites.</p></li>
<li><p>Result set for recent filter.</p></li>
</ul></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>rs/address/list</code></p></td>
<td style="text-align: left;"><p>Call with current filter settings as POST parameter after clicking the search button.</p></td>
<td style="text-align: left;"><ul>
<li><p>Result set matching the given filter settings.</p></li>
</ul></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>rs/address/filter/create</code></p></td>
<td style="text-align: left;"><p>For creating a new favorite filter. The current filter settings of the UI including the specified name of the new filter are required.</p></td>
<td style="text-align: left;"><ul>
<li><p>filter (new current filter)</p></li>
</ul></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>rs/address/filter/select?id={filterId}</code></p></td>
<td style="text-align: left;"><p>For selecting a previous stored favorite filter. Same parameter as for initialList will be returned.</p></td>
<td style="text-align: left;"><ul>
<li><p>UI layout</p></li>
<li><p>New filter settings from selected favorite.</p></li>
<li><p>Result set matching the new selected filter.</p></li>
</ul></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>rs/address/filter/update</code></p></td>
<td style="text-align: left;"><p>For updating the current filter with the new filter settings done by the user.</p></td>
<td style="text-align: left;"></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>rs/address/filter/delete</code></p></td>
<td style="text-align: left;"><p>For deleting a favorite filter.</p></td>
<td style="text-align: left;"><ul>
<li><p>Modified list of available favorites.</p></li>
</ul></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>rs/address/filter/reset</code></p></td>
<td style="text-align: left;"><p>Resets the current filter by default values.</p></td>
<td style="text-align: left;"><ul>
<li><p>The default filter.</p></li>
</ul></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>rs/address/reindexFull</code></p></td>
<td style="text-align: left;"><p>For rebuilding the full search index for the enties (e. g. all addresses).</p></td>
<td style="text-align: left;"></td>
</tr>
</tbody>
</table>

### Example of json format

**rs/address/initialList**

    {
      "ui": {
        "title": "Address list",
        "layout": [
          {
            "id": "resultSet",
            "type": "TABLE",
            "key": "el-1",
            "columns": [
              {
                "id": "address.lastUpdate",
                "title": "modified",
                "dataType": "DATE",
                "sortable": true,
                "formatter": "DATE",
                "type": "TABLE_COLUMN",
                "key": "el-2"
              },
         ...
        "namedContainers": [
         {
            "id": "searchFilter",
            "content": [
              {
                "id": "name",
                "filterType": "STRING",
                "label": "Name",
                "type": "FILTER_ELEMENT",
                "key": "name"
              },
              {
                "id": "contactStatus",
                "type": "SELECT",
                "key": null,
                "required": true,
                "multi": true,
                "label": "Contact status",
                "labelProperty": "label",
                "valueProperty": "value",
                "values": [
                  {
                    "value": "ACTIVE",
                    "label": "active"
                  },
                  {
                    "value": "NON_ACTIVE",
                    "label": "non-active"
                  },
                  ...
                ]
              },
              {
                "id": "modifiedByUser",
                "label": "modified by",
                "autoCompletion": {
                  "minChars": 2,
                  "url": "user/ac"
                },
                "type": "FILTER_ELEMENT",
                "key": "modifiedByUser",
                "filterType": "OBJECT"
              },
              {
                "id": "modifiedInterval",
                "label": "Time of modification",
                "openInterval": true,
                "selectors": [
                  "YEAR",
                  "MONTH",
                  "WEEK",
                  "DAY",
                  "UNTIL_NOW"
                ],
                "type": "FILTER_ELEMENT",
                "key": "modifiedInterval",
                "filterType": "TIME_STAMP"
              },
           ...
       "actions": [
          {
            "id": "reset",
            "title": "Reset",
            "style": "danger",
            "type": "BUTTON",
            "key": "el-17"
          },
          {
            "id": "search",
        ...
        "translations": {
          "select.placeholder": "Select...",
          "task.title.list.select": "Select structure element",
          "favorites": "Favorites",
          "favorite.addNew": "Add new favorite",
         ...
        "pageMenu": [
          {
            "id": "address.writeSMS",
            "title": "Write a text message",
            "i18nKey": "address.tooltip.writeSMS",
            "url": "wa/sendSms"
          },
          ...
      "data": {
        "resultSet": [
          {
            "address": {
              "name": "Reinhard",
            ...
      "filterFavorites": [
        {
          "id": 3,
          "name": "People of Kassel"
        },
        ...

Explanation

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p><code>ui</code></p></td>
<td style="text-align: left;"><p>Contains the page title and the layout information for the dynamic layout render engine (ReactJS).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>namedContainer</code></p></td>
<td style="text-align: left;"><p>Contains containers usable by the front-end, such as search filter and filter options.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>actions</code></p></td>
<td style="text-align: left;"><p>The action buttons to display and handle by the front-end.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>translations</code></p></td>
<td style="text-align: left;"><p>All required translations usable by the front-end for i18n.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>pageMenu</code></p></td>
<td style="text-align: left;"><p>The context menu to show on the list page including the actions to execute by the front-end.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>data</code></p></td>
<td style="text-align: left;"><p>Contains the result set with all result data matching the current filter settings.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>filterFavorites</code></p></td>
<td style="text-align: left;"><p>List of personal named filter favorites customizable by the user.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>key</code></p></td>
<td style="text-align: left;"><p>The key attribute is a service for the React client: a unique key for elements of a collection is needed by ReactJS.</p></td>
</tr>
</tbody>
</table>

Visit <http://localhost:8080/rs/address/initialList> for a full example.
Please login in your browser first: <http://localhost:8080>

## Standard edit pages

Available REST calls:

<table>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Rest call</p></td>
<td style="text-align: left;"><p>Description</p></td>
<td style="text-align: left;"><p>Return values</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>rs/address/{id}</code></p></td>
<td style="text-align: left;"><p>Only the entity with the given id will be returned (not used by React frontend).</p></td>
<td style="text-align: left;"><ul>
<li><p>The pure data object.</p></li>
</ul></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>rs/address/edit?id={id}</code></p></td>
<td style="text-align: left;"><p>Initial call for editing. If id is not given, the layout for creating a new object is returned.</p></td>
<td style="text-align: left;"><ul>
<li><p>UI layout including action buttons.</p></li>
<li><p>The object data (default values for new objects or all values for editing existing objects).</p></li>
</ul></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>rs/address/history/{id}</code></p></td>
<td style="text-align: left;"><p>For getting the complete history of changes of the given object.</p></td>
<td style="text-align: left;"><ul>
<li><p>All entries of the history of changes.</p></li>
</ul></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>rs/address/ac?property={property}&amp;search={search}</code></p></td>
<td style="text-align: left;"><p>Autocompletion: for searching all used property values (e. g. used locations of time sheets).</p></td>
<td style="text-align: left;"><ul>
<li><p>All matching property values.</p></li>
</ul></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>rs/address/ac?&amp;search={search}</code></p></td>
<td style="text-align: left;"><p>Autocompletion: for full text searching all objects matching the given search string.</p></td>
<td style="text-align: left;"><ul>
<li><p>All matching objects (e. g. addresses).</p></li>
</ul></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>rs/address/history/{id}</code></p></td>
<td style="text-align: left;"><p>For getting the complete history of changes of the given object.</p></td>
<td style="text-align: left;"><ul>
<li><p>All entries of the history of changes.</p></li>
</ul></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>rs/address/saveorupdate</code></p></td>
<td style="text-align: left;"><p>For saving or updating objects.</p></td>
<td style="text-align: left;"><ul>
<li><p>The new URL to redirect, if any.</p></li>
</ul></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>rs/address/clone</code></p></td>
<td style="text-align: left;"><p>For cloning the current displayed object. Returns the initial UI layout for new objects including the create button instead of delete and update.</p></td>
<td style="text-align: left;"><ul>
<li><p>UI layout including action buttons.</p></li>
<li><p>The object as clone without id.</p></li>
</ul></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>rs/address/markAsDeleted</code></p></td>
<td style="text-align: left;"><p>For marking historizable objects as deleted. Fails for non historizable entities.</p></td>
<td style="text-align: left;"></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>rs/address/delete</code></p></td>
<td style="text-align: left;"><p>For deleting objects from the data base without undo option. Fails for historizable entities.</p></td>
<td style="text-align: left;"></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>rs/address/cancel</code></p></td>
<td style="text-align: left;"><p>Cancel the edit page.</p></td>
<td style="text-align: left;"><ul>
<li><p>The new URL to redirect to.</p></li>
</ul></td>
</tr>
</tbody>
</table>

### Example of json format

    {
      "data": {
        "contactStatus": "ACTIVE",
        "name": "Schmidt",
        ...
      },
      "ui": {
        "title": "Edit address",
        "layout": [
          {
            "content": [
              {
                "length": 12,
                "type": "FIELDSET",
                "key": "el-2",
                "content": [
                  ...
                {
                  "id": "addressStatus",
                  "type": "SELECT",
                  "key": "el-9",
                  "required": true,
                  "label": "Address status",
                  "values": [
                      {
                         "value": "UPTODATE",
                         ...
                      }]
                },
                ...
                {
                  "id": "name",
                  "maxLength": 255,
                  "required": true,
                  "focus": true,
                  "dataType": "STRING",
                  "label": "Name",
                  "type": "INPUT",
                  "key": "el-24"
                },
                ...
        "actions": [
          {
            "id": "cancel",
            "title": "Cancel",
            "style": "danger",
            "type": "BUTTON",
            "key": "el-137",
            "responseAction": {
              "url": "address/cancel",
              "targetType": "POST"
            }
          },
          {
            "id": "markAsDeleted",
            "title": "Mark as deleted",
            "style": "warning",
            "type": "BUTTON",
            "key": "el-138",
            "responseAction": {
              "url": "address/markAsDeleted",
              "targetType": "DELETE"
            }
          },
          {
            "id": "update",
            "title": "Save",
            "style": "primary",
            "default": true,
            "type": "BUTTON",
            "key": "el-140",
            "responseAction": {
              "url": "address/saveorupdate",
              "targetType": "POST"
            }
          }
          ...
        ],
        "pageMenu": [
          {
            "id": "address.printView",
            "title": "print view",
            "i18nKey": "printView",
            "url": "wa/addressView?id=2",
            "type": "REDIRECT"
          },
          ...
        "translations": {
          "file.upload.dropArea": "Select a file, or drop it here.",
          "label.historyOfChanges": "History of changes",
          ...

Explanation

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p><code>data</code></p></td>
<td style="text-align: left;"><p>Contains the result set with all result data matching the current filter settings.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>ui</code></p></td>
<td style="text-align: left;"><p>Contains the page title and the layout information for the dynamic layout render engine (ReactJS).</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>actions</code></p></td>
<td style="text-align: left;"><p>The action buttons to display and handle by the front-end.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>pageMenu</code></p></td>
<td style="text-align: left;"><p>The context menu to show on the list page including the actions to execute by the front-end.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>translations</code></p></td>
<td style="text-align: left;"><p>All required translations usable by the front-end for i18n.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>key</code></p></td>
<td style="text-align: left;"><p>The key attribute is a service for the React client: a unique key for elements of a collection is needed by ReactJS.</p></td>
</tr>
</tbody>
</table>

Visit <http://localhost:8080/rs/address/edit?id=128> for a full example.
May-be another id is needed, so refer the initilList for address id’s
(`data.resultSet.address.id`, not tenant’s id)! Please login in your
browser first: <http://localhost:8080>

### Magic filter

An example filter for querying a result set:

![](images/Books-magicfilter.png)

**MagicFilter.json**

       "entries": [
          {
            "search": "fin"
          },
          {
            "field": "modifiedByUser",
            "value": {
              "id": 2,
              "deleted": false
            }
          },
          {
            "field": "title",
            "search": "java",
            "matchType": "STARTS_WITH"
          },
          {
            "field": "modifiedInterval",
            "fromValue": "2019-04-28'T'10:00:05.000Z",
            "toValue": "2019-04-28'T'17:00:05.000Z"
          },
          {
            "field": "yearOfPublishing",
            "fromValue": 2010
          },
          {
            "field": "type",
            "values": [
              "BOOK",
              "MAGAZINE"
            ]
          }
        ]

Explanation for filter settings:

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p><code>"search": "fin"</code></p></td>
<td style="text-align: left;"><p>Full text search (for all fields) with standard <code>matchType=STARTS_WITH</code>: <code>fin*</code></p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>"field": "modifiedByUser"</code></p></td>
<td style="text-align: left;"><p>Selects all entries modified by the given user.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>"field": "title"</code></p></td>
<td style="text-align: left;"><p>Selects entries with the matching title.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>"field": "modifiedInterval"</code></p></td>
<td style="text-align: left;"><p>Selects entries modified in the given time interval.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><code>"field": "yearOfPublishing"</code></p></td>
<td style="text-align: left;"><p>Selects entries with the <code>yearOfPublishing</code> 2010 and newer.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><code>"field": "type"</code></p></td>
<td style="text-align: left;"><p>Selects entries with the type matching one of the given values.</p></td>
</tr>
</tbody>
</table>

### REST-API: JPA entities vs. DTO

For simple objects the JPA objects (BaseDO) may be used for the CRUD
functionality through the REST-API. For more complex objects, especially
if these objects are embedded by other entities (users, tasks etc.) a
DTO (data transfer object) has to be used.

In thanks to Kotlin, the creation of a DTO is very simple and efficient.
Examples:

-   [`Address.kt`](https://github.com/micromata/projectforge/blob/develop/projectforge-rest/src/main/kotlin/org/projectforge/rest/dto/Address.kt)
    The DTO for addresses is needed, because addresses may contain
    images with a special handling.

-   [`User.kt`](https://github.com/micromata/projectforge/blob/develop/projectforge-rest/src/main/kotlin/org/projectforge/rest/dto/User.kt)
    The DTO for users is needed, because user objects are embedded in
    other JPA entities.

-   [`Task.kt`](https://github.com/micromata/projectforge/blob/develop/projectforge-rest/src/main/kotlin/org/projectforge/rest/dto/Task.kt)
    Task is embedded by other entities as well.

The base class `BaseDTO` provides base functionality for the
automatically transformation of DTO and BaseDO.

### Putting all together in Kotlin code

Simple example (books)

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Class (Link)</p></td>
<td style="text-align: left;"><p>Description</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><a href="https://github.com/micromata/projectforge/blob/develop/projectforge-business/src/main/kotlin/org/projectforge/business/book/BookDO.kt"><code>BookDO.kt</code></a></p></td>
<td style="text-align: left;"><p>Defines the entity</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><a href="https://github.com/micromata/projectforge/blob/develop/projectforge-business/src/main/java/org/projectforge/business/book/BookDao.java"><code>BookDao.java</code></a></p></td>
<td style="text-align: left;"><p>Defines access rights and special functionality for books</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><a href="https://github.com/micromata/projectforge/blob/develop/projectforge-rest/src/main/kotlin/org/projectforge/rest/BookRest.kt"><code>BookRest.kt</code></a></p></td>
<td style="text-align: left;"><p>Books with support of ReactJS as well as REST API for CRUD operations</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><a href="https://github.com/micromata/projectforge/blob/develop/projectforge-business/src/main/kotlin/org/projectforge/business/book/BookStatus.kt"><code>BookStatus.kt</code></a></p></td>
<td style="text-align: left;"><p>Enumeration of book status including i18n</p></td>
</tr>
</tbody>
</table>

Nothing more is needed to have a simple entity provided by
ProjectForge!!! No HTML, no JavaScript, nothing else.

Simple example (addresses) with more fields and UI layout with more
fieldsets and columns (supporting different screen resolutions,
responsive).

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Class (Link)</p></td>
<td style="text-align: left;"><p>Description</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><a href="https://github.com/micromata/projectforge/blob/develop/projectforge-business/src/main/kotlin/org/projectforge/business/address/AddressDO.kt"><code>AddressDO.kt</code></a></p></td>
<td style="text-align: left;"><p>Defines the entity</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><a href="https://github.com/micromata/projectforge/blob/develop/projectforge-business/src/main/java/org/projectforge/business/address/AddressDao.java"><code>AddressDao.java</code></a></p></td>
<td style="text-align: left;"><p>Defines access rights and special functionality for addresses</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><a href="https://github.com/micromata/projectforge/blob/develop/projectforge-rest/src/main/kotlin/org/projectforge/rest/AddressRest.kt"><code>AddressRest.kt</code></a></p></td>
<td style="text-align: left;"><p>Addresses with support of ReactJS (responsive) as well as REST API for CRUD operations</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><a href="https://github.com/micromata/projectforge/blob/develop/projectforge-rest/src/main/kotlin/org/projectforge/rest/dto/Address.kt"><code>Address.kt</code></a></p></td>
<td style="text-align: left;"><p>Data transfer object for the client. For simple objects, the BaseDO object may be used for the REST-CRUD functionality. For more complex objects providing special functionality, the usage of a DTO is required/recommended.</p></td>
</tr>
</tbody>
</table>

# Writing own plugins

Refer [ProjectForge-plugins on
GitHub](https://github.com/micromata/projectforge-plugins) for examples.

-   KTMemo as an Kotlin plugin.

-   JMemo as an Java plugin.

## Starting your plugin from command line

1.  Build jar file by calling `mvn clean install`.

2.  Copy jar file to plugins folder of ProjectForge home, e. g.
    `/home/kai/ProjectForge/plugins`.

3.  Tell ProjectForge where it is. You may have to options:

    1.  Run ProjectForge from command line with option
        `-Dloader.home=/home/kai/ProjectForge`, or

    2.  Set the environment variable before starting ProjectForge:
        `export LOADER_HOME=/home/kai/ProjectForge`.

4.  Start ProjectForge and activate the plugin as admin in the
    ProjectForge’s web app under menu Admin→plugins.

5.  Restart ProjectForge.

## Starting and debugging in IntelliJ

The loader home stuff doesn’t work if you start ProjectForge’s main in
IntelliJ. Following the solution described in [Stack
overflow](https://stackoverflow.com/questions/37833877/intellij-spring-boot-propertieslauncher):
1. Enable profile `intellij-properties-launcher` in maven tab. 2. Edit
launch configuration: a. Main class:
`org.springframework.boot.loader.PropertiesLauncher`. Please note, the
configuration is marked faulty, but it works. b. VM options:
`-Xms2000m -Xmx2000m -Dloader.home=/home/kai/ProjectForge -Dloader.main=org.projectforge.start.ProjectForgeApplication`
c. Environment variables: `LOADER_HOME=/home/kai/ProjectForge`

For debugging you may attach the plugin to your IntelliJ classpath.

## Limitations in Plugins

-   Own customized components are not yet supported in UILayout for
    external plugins. We’re working on this issue.

All other functionality seems to be available.

# Misc

## Code style

The standard IntelliJ coding style is used, but tab width and indent
with is 2 and continuation indent is 4.

Please import code style: `misc/IntelliJ/CodeStyle.xml`

## Working with different data bases for testing

\`\`\` docker run -e PGPASSWORD=$PGPASSWORD -it --rm --link
projectforge-postgres:postgres postgres:11.2 psql -h postgres -U
projectforge \`\`\` \`\`\` create user pf2 password *secret*; CREATE
DATABASE pf2; GRANT ALL PRIVILEGES ON DATABASE pf2 TO pf2; \`\`\`

