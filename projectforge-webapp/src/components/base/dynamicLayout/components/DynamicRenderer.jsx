import React from 'react';
import DynamicCustomized from './customized';
import DynamicAgGrid from './table/DynamicAgGrid';
import DynamicAlert from './DynamicAlert';
import DynamicAttachmentList from './input/DynamicAttachmentList';
import DynamicButton from './DynamicButton';
import DynamicFieldset from './DynamicFieldset';
import DynamicGroup from './DynamicGroup';
import DynamicInlineGroup from './DynamicInlineGroup';
import DynamicLabel from './DynamicLabel';
import DynamicList from './DynamicList';
import DynamicCheckbox from './input/DynamicCheckbox';
import DynamicInputResolver from './input/DynamicInputResolver';
import CustomizedJobsMonitor from './customized/components/CustomizedJobsMonitor';
import DynamicRadioButton from './input/DynamicRadioButton';
import DynamicPollRadioButton from './input/DynamicPollRadioButton';
import DynamicRating from './input/DynamicRating';
import DynamicReadonlyField from './input/DynamicReadonlyField';
import DynamicSpacer from './DynamicSpacer';
import DynamicTextArea from './input/DynamicTextArea';
import DynamicReactCreatableSelect from './select/DynamicReactCreatableSelect';
import DynamicReactSelect from './select/DynamicReactSelect';
import DynamicTable from './table/DynamicTable';
import DynamicListPageTable from './table/DynamicListPageTable';
import DynamicListPageAgGrid from './table/DynamicListPageAgGrid';
import DynamicBadgeList from './DynamicBadgeList';
import DynamicBadge from './DynamicBadge';
import DynamicDropArea from './input/DynamicDropArea';
import DynamicEditor from './input/DynamicEditor';

const components = {};

export const registerComponent = (type, tag) => {
    components[type] = tag;
};

// Renders the components out of a content array.
export default function DynamicRenderer(content) {
    if (!content) {
        return null;
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
}

// register default components
registerComponent('ALERT', DynamicAlert);
registerComponent('ATTACHMENT_LIST', DynamicAttachmentList);
registerComponent('BADGE', DynamicBadge);
registerComponent('BADGE_LIST', DynamicBadgeList);
registerComponent('BUTTON', DynamicButton);
registerComponent('CHECKBOX', DynamicCheckbox);
registerComponent('COL', DynamicGroup);
registerComponent('DROP_AREA', DynamicDropArea);
registerComponent('FRAGMENT', DynamicGroup);
registerComponent('GROUP', DynamicGroup);
registerComponent('INLINE_GROUP', DynamicInlineGroup);
registerComponent('LIST', DynamicList);
registerComponent('RADIOBUTTON', DynamicRadioButton);
registerComponent('POLL_RADIOBUTTON', DynamicPollRadioButton);
registerComponent('ROW', DynamicGroup);
registerComponent('CUSTOMIZED', DynamicCustomized);
registerComponent('FIELDSET', DynamicFieldset);
registerComponent('INPUT', DynamicInputResolver);
registerComponent('LABEL', DynamicLabel);
registerComponent('PROGRESS', CustomizedJobsMonitor);
registerComponent('RATING', DynamicRating);
registerComponent('READONLY_FIELD', DynamicReadonlyField);
registerComponent('SELECT', DynamicReactSelect);
registerComponent('SPACER', DynamicSpacer);
registerComponent('CREATABLE_SELECT', DynamicReactCreatableSelect);
registerComponent('TABLE', DynamicTable);
registerComponent('TABLE_LIST_PAGE', DynamicListPageTable);
registerComponent('AG_GRID', DynamicAgGrid);
registerComponent('AG_GRID_LIST_PAGE', DynamicListPageAgGrid);
registerComponent('TEXTAREA', DynamicTextArea);
registerComponent('EDITOR', DynamicEditor);
