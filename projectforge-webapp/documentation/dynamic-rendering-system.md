# ProjectForge Dynamic Rendering System

This document describes ProjectForge's dynamic rendering system which enables server-driven UI generation, allowing backend developers to define complex UI layouts that are rendered consistently by the frontend.

## Table of Contents

1. [Overview](#overview)
2. [Backend Implementation](#backend-implementation)
   - [Core Layout Classes](#core-layout-classes)
   - [Layout Generation Process](#layout-generation-process)
   - [REST API Endpoints](#rest-api-endpoints)
3. [Layout Data Structure](#layout-data-structure)
   - [UILayout Structure](#uilayout-structure)
   - [Component Definitions](#component-definitions)
   - [Example Layout](#example-layout)
4. [Frontend Implementation](#frontend-implementation)
   - [Dynamic Renderer](#dynamic-renderer)
   - [Component Registration](#component-registration)
   - [Dynamic Layout Context](#dynamic-layout-context)
5. [Available Components](#available-components)
   - [Basic Components](#basic-components)
   - [Input Components](#input-components) 
   - [Layout Components](#layout-components)
   - [Advanced Components](#advanced-components)
6. [Creating Custom Components](#creating-custom-components)
7. [Best Practices](#best-practices)

## Overview

The dynamic rendering system decouples UI presentation from business logic by allowing the backend to define the complete UI layout structure. This approach offers several advantages:

- Consistent UI across the application
- Single source of truth for UI structure 
- Localization handled at the backend
- Dynamic form behavior based on user roles and permissions
- The ability to add new UI screens without frontend changes

This server-driven UI approach enables rapid development of new features while maintaining a consistent user experience.

## Backend Implementation

### Core Layout Classes

The backend implementation centers around several key classes:

- **UILayout**: Main class that represents a UI layout with a hierarchy of UI elements, actions, and containers. Provides methods like `add()`, `addAction()`, and `getElementById()` to manipulate the UI structure.

- **LayoutBuilder**: Helper for creating layouts with methods that create rows, columns, and elements based on entity properties.

- **LayoutContext**: Provides context for generating UI elements, holds information about data object classes, and manages element registration.

- **LayoutUtils**: Central utility class for layout processing that adds translations, processes standard actions, and ensures consistent styling.

### Layout Generation Process

The system uses reflection and annotations to automate UI generation:

1. **ElementsRegistry**:
   - Auto-detects appropriate UI elements based on Java/Kotlin property types
   - Creates the right element type (date picker, dropdown, text field, etc.)
   - Uses metadata like field length to determine the right type of input

2. **PropertyInfo Annotations**:
   - Properties in domain objects can be annotated with `@PropertyInfo`
   - Provides i18n keys, validation rules, and UI display hints

3. **Layout Generation**:
   - Domain objects' properties are analyzed
   - UI elements are created for each relevant property
   - Elements are arranged in a layout structure (rows/columns/groups)
   - Actions (buttons) are added based on permissions

### REST API Endpoints

UI layouts are served through REST endpoints:

- Base class `AbstractPagesRest` provides common functionality
- Entity-specific endpoints extend the base class
- Standard endpoints include:
  - `GET /api/{entity}/list`: Returns list layout with search filters
  - `GET /api/{entity}/edit/{id}`: Returns edit form layout  
  - `POST /api/{entity}/edit`: Handles form submissions with validation

The endpoint returns a `FormLayoutData` object containing:
- UI layout with all elements and their properties
- Data to populate the form
- Translation strings
- Validation errors (if any)

## Layout Data Structure

### UILayout Structure

The layout data sent to the frontend has this general structure:

```json
{
  "ui": {
    "title": "Edit User",
    "layout": [
      {
        "type": "ROW",
        "key": "r1",
        "content": [
          {
            "type": "COL",
            "key": "c1",
            "length": 6,
            "content": [
              {
                "type": "INPUT",
                "key": "username",
                "label": "Username",
                "required": true,
                "maxLength": 100
              }
            ]
          }
        ]
      }
    ],
    "actions": [
      {
        "type": "BUTTON",
        "key": "save",
        "title": "Save",
        "color": "primary"
      }
    ],
    "translations": {
      "username.label": "Username",
      "save.title": "Save"
    }
  },
  "data": {
    "username": "admin"
  }
}
```

### Component Definitions

Each component in the layout is defined with:
- **type**: The component type (e.g., "INPUT", "BUTTON", "ROW")
- **key**: Unique identifier for the element, used for data binding
- **properties**: Type-specific properties (e.g., label, required, maxLength)

Some components can contain other components through a "content" array.

### Example Layout

Here's a simplified example of a layout for an address edit form:

```json
{
  "layout": [
    {
      "type": "FIELDSET",
      "key": "fs1",
      "title": "Person",
      "content": [
        {
          "type": "ROW",
          "key": "r1",
          "content": [
            {
              "type": "COL",
              "key": "c1",
              "length": 6,
              "content": [
                {
                  "type": "INPUT",
                  "key": "firstName",
                  "label": "First name",
                  "required": true
                }
              ]
            },
            {
              "type": "COL",
              "key": "c2",
              "length": 6,
              "content": [
                {
                  "type": "INPUT",
                  "key": "lastName",
                  "label": "Last name",
                  "required": true
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

## Frontend Implementation

### Dynamic Renderer

The core of the frontend implementation is the `DynamicRenderer`, which:
- Takes an array of component definitions
- Maps each "type" to a registered React component
- Renders the component tree recursively
- Passes properties to the appropriate components

```jsx
// Simplified example of the renderer
export default function DynamicRenderer(content) {
  if (!content) {
    return null;
  }

  return (
    <>
      {content.map(({ type, key, ...props }) => {
        const Tag = components[type];
        const componentKey = `dynamic-layout-${key}`;

        if (!Tag) {
          return (
            <span key={componentKey}>
              {`Type ${type} is not implemented in DynamicRenderer.`}
            </span>
          );
        }

        return (
          <Tag
            key={componentKey}
            type={type}
            {...props}
          />
        );
      })}
    </>
  );
}
```

### Component Registration

Components register themselves to be available for dynamic rendering:

```jsx
const components = {};

export const registerComponent = (type, tag) => {
  components[type] = tag;
};

// Register built-in components
registerComponent('INPUT', DynamicInputResolver);
registerComponent('SELECT', DynamicReactSelect);
registerComponent('BUTTON', DynamicButton);
// ... and so on
```

### Dynamic Layout Context

A React context provides the layout infrastructure:

- **Data management**: Maintains form values and validation state
- **Action handling**: Processes button clicks and other interactions
- **Layout rendering**: Renders component trees based on layout data
- **Translations**: Provides localized strings to components

```jsx
export const DynamicLayoutContext = React.createContext({
  callAction: () => {},
  data: {},
  isFetching: false,
  options: {
    disableLayoutRendering: false,
    displayPageMenu: true,
    setBrowserTitle: true,
    showActionButtons: true,
    showPageMenuTitle: true,
  },
  renderLayout: () => null,
  setData: () => {},
  setVariables: () => {},
  ui: {
    translations: {},
  },
  validationErrors: [],
  variables: {},
});
```

## Available Components

### Basic Components

| Type | Description |
|------|-------------|
| `LABEL` | Displays text label |
| `BUTTON` | Interactive button for actions |
| `BADGE` | Small label with background color |
| `BADGE_LIST` | Collection of badges |
| `ALERT` | Displays notification message |
| `READONLY_FIELD` | Non-editable text display |

### Input Components

| Type | Description |
|------|-------------|
| `INPUT` | Text input field |
| `CHECKBOX` | Boolean checkbox |
| `RADIOBUTTON` | Single selection from options |
| `SELECT` | Dropdown selection component |
| `CREATABLE_SELECT` | Dropdown with ability to create new options |
| `TEXTAREA` | Multi-line text input |
| `EDITOR` | Rich text editor |
| `RATING` | Star rating input |
| `DROP_AREA` | File upload dropzone |
| `ATTACHMENT_LIST` | Displays attached files |

### Layout Components

| Type | Description |
|------|-------------|
| `ROW` | Horizontal container |
| `COL` | Column within a row |
| `GROUP` | Generic container |
| `FRAGMENT` | Container without DOM element |
| `FIELDSET` | Section with title |
| `SPACER` | Adds vertical space |
| `LIST` | Renders a list of items |

### Advanced Components

| Type | Description |
|------|-------------|
| `TABLE` | Displays tabular data |
| `TABLE_LIST_PAGE` | Table for list pages |
| `AG_GRID` | Advanced grid component |
| `AG_GRID_LIST_PAGE` | AG Grid for list pages |
| `CUSTOMIZED` | Custom component with special behavior |
| `PROGRESS` | Progress indicator |

## Creating Custom Components

To create a custom component for the dynamic layout system:

1. Create a React component that accepts the dynamic layout properties
2. Register it with the `registerComponent` function
3. Use it in the backend layout definition

Example:

```jsx
import React from 'react';
import { registerComponent } from './DynamicRenderer';

function CustomComponent({ label, value, options, ...props }) {
  // Implement your custom rendering logic
  return (
    <div className="custom-component">
      <span className="label">{label}</span>
      <div className="value">{value}</div>
      {/* Additional rendering */}
    </div>
  );
}

// Register the component
registerComponent('CUSTOM_COMPONENT', CustomComponent);

export default CustomComponent;
```

## Best Practices

When working with the dynamic rendering system:

1. **Backend Development**:
   - Group related fields in fieldsets for better organization
   - Use consistent layouts for similar entities
   - Add proper translations for all labels and messages
   - Leverage validation annotations for automatic form validation

2. **Frontend Development**:
   - Keep components focused on presentation, not business logic
   - Use the layout context for data access and modifications
   - Ensure components handle loading and error states gracefully
   - Follow existing styling patterns for consistency

3. **Performance Considerations**:
   - Keep layout definitions as small as possible
   - Use lazy loading for expensive components
   - Optimize re-renders with proper React patterns
   - Consider pagination for large data sets

4. **Extending the System**:
   - Document new component types
   - Add new components to the standard component registration
   - Test with various layouts and data patterns
   - Consider accessibility when implementing new components