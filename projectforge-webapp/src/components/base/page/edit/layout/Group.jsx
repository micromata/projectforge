import React from 'react';
import PropTypes from 'prop-types';
import { Col, FormGroup, Row } from '../../../../design';
import revisedRandomId from '../../../../../utilities/revisedRandomId';
import LayoutLabel from './Label';
import LayoutInput from './Input';

// TODO: COLLECT INPUT IN PARENT
function LayoutGroup({ content, type, length }) {
    let GroupTag;
    const groupProperties = {};

    switch (type) {
        case 'group':
            GroupTag = FormGroup;
            groupProperties.row = true;
            break;
        case 'row':
            GroupTag = Row;
            break;
        case 'col':
            GroupTag = Col;
            groupProperties.sm = length;
            break;
        default:
            GroupTag = React.Fragment;
    }

    return (
        <GroupTag {...groupProperties}>
            {content.map((component) => {
                let Tag;

                switch (component.type) {
                    case 'label':
                        Tag = LayoutLabel;
                        break;
                    case 'text':
                    case 'select':
                    case 'checkbox':
                        Tag = LayoutInput;
                        break;
                    case 'group':
                    case 'row':
                    case 'col':
                        Tag = LayoutGroup;
                        break;
                    default:
                        Tag = LayoutGroup;
                }

                return (
                    <Tag
                        key={`layout-group-component-${revisedRandomId()}`}
                        {...component}
                    />
                );
            })}
        </GroupTag>
    );
}

LayoutGroup.propTypes = {
    // PropType validation with type array has to be allowed here.
    // Otherwise it will create an endless loop of groups.
    /* eslint-disable-next-line react/forbid-prop-types */
    content: PropTypes.array,
    type: PropTypes.string,
    length: PropTypes.number,
};

LayoutGroup.defaultProps = {
    content: [],
    type: 'container',
    length: undefined,
};

export default LayoutGroup;
