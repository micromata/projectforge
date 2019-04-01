import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { dataPropType } from '../../../../utilities/propTypes';
import { CheckBox, Input, TextArea } from '../../../design';

class LayoutInput extends Component {
    constructor(props) {
        super(props);

        this.handleInputChange = this.handleInputChange.bind(this);
        this.handleSelectChange = this.handleSelectChange.bind(this);
    }

    handleInputChange(event) {
        const { id, changeDataField, type } = this.props;

        if (!id || !changeDataField) {
            return;
        }

        let newValue;

        switch (type) {
            case 'CHECKBOX':
                newValue = event.target.checked;
                break;
            default:
                newValue = event.target.value;
        }

        changeDataField(id, newValue);
    }

    handleSelectChange(value) {
        const { id, changeDataField } = this.props;

        changeDataField(id, value);
    }

    render() {
        const {
            id,
            data,
            label,
            maxLength,
            required,
            type,
            validation,
        } = this.props;

        let Tag;
        const properties = {};
        const value = data[id] || '';


        switch (type) {
            case 'INPUT':
                Tag = Input;
                break;
            case 'TEXTAREA':
                Tag = TextArea;
                break;
            case 'CHECKBOX':
                Tag = CheckBox;
                properties.checked = value || false;
                break;
            default:
                Tag = 'div';
        }

        if (
            type !== 'CHECKBOX'
            && ((required && !value) || (maxLength && value.length > maxLength))
        ) {
            properties.color = 'danger';
        }


        if (validation[id]) {
            properties.color = 'danger';
            properties.additionalLabel = validation[id];
        }

        return (
            <Tag
                label={label}
                id={id}
                {...properties}
                onChange={this.handleInputChange}
                value={value}
            />
        );
    }
}

LayoutInput.propTypes = {
    label: PropTypes.string.isRequired,
    changeDataField: PropTypes.func,
    data: dataPropType.isRequired,
    id: PropTypes.string,
    type: PropTypes.string,
    values: PropTypes.arrayOf(PropTypes.shape({
        value: PropTypes.string,
        title: PropTypes.string,
    })),
    maxLength: PropTypes.number,
    required: PropTypes.bool,
    validation: PropTypes.shape({}),
};

LayoutInput.defaultProps = {
    changeDataField: undefined,
    id: undefined,
    type: 'TEXT',
    values: [],
    maxLength: 0,
    required: false,
    validation: {},
};

export default LayoutInput;
