import React from 'react';
import DynamicFieldset from './DynamicFieldset';
import DynamicGroup from './DynamicGroup';
import DynamicLabel from './DynamicLabel';
import DynamicInputResolver from './input/DynamicInputResolver';

// Renders the components out of a content array.
export default (content) => {
    if (!content) {
        return <React.Fragment />;
    }

    return (
        <React.Fragment>
            {content.map(({ type, key, ...props }) => {
                const componentKey = `dynamic-layout-${key}`;
                let Tag;

                // See all allowed types in propTypes.js -> dynamicTypePropType
                switch (type) {
                    case 'COL':
                    case 'ROW':
                        Tag = DynamicGroup;
                        break;
                    case 'FIELDSET':
                        Tag = DynamicFieldset;
                        break;
                    case 'INPUT':
                        Tag = DynamicInputResolver;
                        break;
                    case 'LABEL':
                        Tag = DynamicLabel;
                        break;
                    default:
                        return <React.Fragment key={componentKey} />;
                }

                return (
                    <Tag
                        key={`dynamic-layout-${componentKey}`}
                        type={type}
                        {...props}
                    />
                );
            })}
        </React.Fragment>
    );
};
