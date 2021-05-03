import React from 'react';
import DynamicCustomized from './customized';
import DynamicAlert from './DynamicAlert';
import DynamicAttachmentList from './input/DynamicAttachmentList';
import DynamicButton from './DynamicButton';
import DynamicFieldset from './DynamicFieldset';
import DynamicGroup from './DynamicGroup';
import DynamicLabel from './DynamicLabel';
import DynamicList from './DynamicList';
import DynamicCheckbox from './input/DynamicCheckbox';
import DynamicInputResolver from './input/DynamicInputResolver';
import DynamicRadioButton from './input/DynamicRadioButton';
import DynamicRating from './input/DynamicRating';
import DynamicReadonlyField from './input/DynamicReadonlyField';
import DynamicTextArea from './input/DynamicTextArea';
import DynamicReactSelect from './select/DynamicReactSelect';
import DynamicTable from './table/DynamicTable';

const components = {};

export const registerComponent = (type, tag) => {
    components[type] = tag;
};

// Renders the components out of a content array.
export default (content) => {
    if (!content) {
        return <></>;
    }

    return (
        <>
            {/* eslint-disable-next-line react/destructuring-assignment */}
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
};

// register default components
registerComponent('ALERT', DynamicAlert);
registerComponent('ATTACHMENT_LIST', DynamicAttachmentList);
registerComponent('BUTTON', DynamicButton);
registerComponent('CHECKBOX', DynamicCheckbox);
registerComponent('COL', DynamicGroup);
registerComponent('FRAGMENT', DynamicGroup);
registerComponent('GROUP', DynamicGroup);
registerComponent('LIST', DynamicList);
registerComponent('RADIOBUTTON', DynamicRadioButton);
registerComponent('ROW', DynamicGroup);
registerComponent('CUSTOMIZED', DynamicCustomized);
registerComponent('FIELDSET', DynamicFieldset);
registerComponent('INPUT', DynamicInputResolver);
registerComponent('LABEL', DynamicLabel);
registerComponent('RATING', DynamicRating);
registerComponent('READONLY_FIELD', DynamicReadonlyField);
registerComponent('SELECT', DynamicReactSelect);
registerComponent('TABLE', DynamicTable);
registerComponent('TEXTAREA', DynamicTextArea);
