import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import revisedRandomId from '../../../../utilities/revisedRandomId';
import { Col, FormGroup, Row } from '../../../design';
import style from '../Page.module.scss';
import LayoutInput from './Input';
import LayoutLabel from './Label';

// TODO: COLLECT INPUT IN PARENT
function LayoutGroup(
    {
        content,
        type,
        length,
        data,
    },
) {
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
            GroupTag = 'div';
    }

    return (
        <GroupTag
            {...groupProperties}
            className={classNames(style.group, groupProperties.className)}
        >
            {content.map((component) => {
                let Tag;

                switch (component.type) {
                    case 'label':
                        Tag = LayoutLabel;
                        break;
                    case 'input':
                    case 'select':
                    case 'checkbox':
                    case 'textarea':
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
                        data={data}
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
    data: PropTypes.shape,
};

LayoutGroup.defaultProps = {
    content: [],
    type: 'container',
    length: undefined,
    data: {},
};

export default LayoutGroup;
