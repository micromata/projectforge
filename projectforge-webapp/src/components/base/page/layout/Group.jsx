import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Col, FormGroup, Row } from '../../../design';
import style from '../Page.module.scss';
import CustomizedLayout from './customized';
import LayoutInput from './Input';
import LayoutLabel from './Label';
import LayoutSelect from './Select';
import LayoutTable from './Table';

function LayoutGroup(
    {
        changeDataField,
        content,
        data,
        length,
        type,
        validation,
    },
) {
    let GroupTag;
    const groupProperties = {};

    switch (type) {
        case 'GROUP':
            GroupTag = FormGroup;
            groupProperties.row = true;
            break;
        case 'ROW':
            GroupTag = Row;
            break;
        case 'COL':
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
                    case 'LABEL':
                        Tag = LayoutLabel;
                        break;
                    case 'INPUT':
                    case 'CHECKBOX':
                    case 'TEXTAREA':
                        Tag = LayoutInput;
                        break;
                    case 'SELECT':
                        Tag = LayoutSelect;
                        break;
                    case 'GROUP':
                    case 'ROW':
                    case 'COL':
                        Tag = LayoutGroup;
                        break;
                    case 'TABLE':
                        Tag = LayoutTable;
                        break;
                    case 'CUSTOMIZED':
                        Tag = CustomizedLayout;
                        break;
                    default:
                        Tag = LayoutGroup;
                }

                return (
                    <Tag
                        changeDataField={changeDataField}
                        data={data}
                        validation={validation}
                        {...component}
                        key={`layout-group-component-${component.key}-${data.id}`}
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
    validation: PropTypes.shape({}),
};

LayoutGroup.defaultProps = {
    changeDataField: undefined,
    content: [],
    type: 'CONTAINER',
    length: undefined,
    data: {},
    validation: {},
};

export default LayoutGroup;
