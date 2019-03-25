import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Col, FormGroup, Row } from '../../../design';
import style from '../Page.module.scss';
import LayoutInput from './Input';
import LayoutLabel from './Label';
import LayoutSelect from './Select';
import LayoutTable from './Table';

// TODO: COLLECT INPUT IN PARENT
function LayoutGroup(
    {
        changeDataField,
        content,
        data,
        length,
        type,
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
                    case 'checkbox':
                    case 'textarea':
                        Tag = LayoutInput;
                        break;
                    case 'select':
                        Tag = LayoutSelect;
                        break;
                    case 'group':
                    case 'row':
                    case 'col':
                        Tag = LayoutGroup;
                        break;
                    case 'table':
                        Tag = LayoutTable;
                        break;
                    default:
                        Tag = LayoutGroup;
                }

                return (
                    <Tag
                        changeDataField={changeDataField}
                        data={data}
                        {...component}
                        key={`layout-group-component-${component.key}`}
                    />
                );
            })}
        </GroupTag>
    );
}

LayoutGroup.propTypes = {
    changeDataField: PropTypes.func,
    // PropType validation with type array has to be allowed here.
    // Otherwise it will create an endless loop of groups.
    /* eslint-disable-next-line react/forbid-prop-types */
    content: PropTypes.array,
    type: PropTypes.string,
    length: PropTypes.number,
    data: PropTypes.shape({}),
};

LayoutGroup.defaultProps = {
    changeDataField: undefined,
    content: [],
    type: 'container',
    length: undefined,
    data: {},
};

export default LayoutGroup;
