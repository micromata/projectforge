import PropTypes from 'prop-types';
import React from 'react';
import { Col, FormGroup, Row } from '../../../design';
import CustomizedLayout from './customized';
import LayoutDayPickerInput from './LayoutDayPickerInput';
import LayoutInput from './LayoutInput';
import LayoutLabel from './LayoutLabel';
import ReactSelect from './ReactSelect';
import LayoutTable from './table';
import Timestamp from './Timestamp';

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
    const groupProperties = {};
    const { translations } = props;

    const children = (
        <React.Fragment>
            {title
                ? <legend>{title}</legend>
                : undefined}
            {content.map((component) => {
                let Tag;
                const properties = {};

                switch (component.type) {
                    case 'LABEL':
                        Tag = LayoutLabel;
                        break;
                    case 'INPUT':
                        if (component.dataType === 'DATE') {
                            Tag = LayoutDayPickerInput;
                        } else if (component.dataType === 'TIMESTAMP') {
                            Tag = Timestamp;
                        } else {
                            Tag = LayoutInput;
                        }
                        break;
                    case 'CHECKBOX':
                    case 'TEXTAREA':
                        Tag = LayoutInput;
                        break;
                    case 'SELECT':
                        Tag = ReactSelect;
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
                    case 'MULTI_SELECT':
                        Tag = ReactSelect;
                        properties.isMulti = true;
                        break;
                    case 'CUSTOMIZED':
                        Tag = CustomizedLayout;
                        break;
                    default:
                        return (
                            <p key={`layout-group-unknown-component-${component.key}-${data.id}`}>
                                {`${component.type} NOT IMPLEMENTED YET.`}
                            </p>
                        );
                }
                return (
                    <Tag
                        data={data}
                        {...props}
                        {...component}
                        {...properties}
                        {...translations}
                        key={`layout-group-component-${component.key}-${data.id}`}
                    />
                );
            })}
        </React.Fragment>
    );

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
            return (
                <Col sm={length}>
                    <fieldset>
                        {children}
                    </fieldset>
                </Col>
            );
        default:
            GroupTag = 'div';
    }

    return (
        <GroupTag {...groupProperties}>
            {children}
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
    translations: PropTypes.shape({}),
};

LayoutGroup.defaultProps = {
    changeDataField: undefined,
    content: [],
    data: {},
    length: undefined,
    title: undefined,
    type: 'CONTAINER',
    validation: {},
    translations: {},
};

export default LayoutGroup;
