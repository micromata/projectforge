import PropTypes from 'prop-types';
import React from 'react';
import { Col, FormGroup, Row } from '../../../design';
import CustomizedLayout from './customized';
import LayoutInput from './Input';
import LayoutLabel from './Label';
import LayoutSelect from './Select';
import LayoutTable from './table';

function LayoutGroup(
    {
        content,
        data,
        length,
        title,
        type,
        ...props
    },
) {
    let GroupTag;
    let SubGroupTag = React.Fragment;
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
        case 'FIELDSET':
            GroupTag = Col;
            groupProperties.sm = length;
            SubGroupTag = 'fieldset';
            break;
        default:
            GroupTag = 'div';
    }

    return (
        <GroupTag {...groupProperties}>
            <SubGroupTag>
                {title
                    ? <legend>{title}</legend>
                    : undefined}
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
                        case 'FIELDSET':
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
                            data={data}
                            {...props}
                            {...component}
                            key={`layout-group-component-${component.key}-${data.id}`}
                        />
                    );
                })}
            </SubGroupTag>
        </GroupTag>
    );
}

LayoutGroup.propTypes = {
    changeDataField: PropTypes.func,
    // PropType validation with type array has to be allowed here.
    // Otherwise it will create an endless loop of groups.
    /* eslint-disable-next-line react/forbid-prop-types */
    content: PropTypes.array,
    data: PropTypes.shape({}),
    length: PropTypes.number,
    title: PropTypes.string,
    type: PropTypes.string,
    validation: PropTypes.shape({}),
};

LayoutGroup.defaultProps = {
    changeDataField: undefined,
    content: [],
    data: {},
    length: undefined,
    title: undefined,
    type: 'CONTAINER',
    validation: {},
};

export default LayoutGroup;
