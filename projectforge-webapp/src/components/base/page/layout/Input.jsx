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
            additionalLabel,
            data,
            focus,
            id,
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

        if (focus) {
            properties.autoFocus = true;
        }

        return (
            <Tag
                additionalLabel={additionalLabel}
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
    data: dataPropType.isRequired,
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    changeDataField: PropTypes.func,
    focus: PropTypes.bool,
    id: PropTypes.string,
    maxLength: PropTypes.number,
    required: PropTypes.bool,
    type: PropTypes.string,
    validation: PropTypes.shape({}),
    values: PropTypes.arrayOf(PropTypes.shape({
        value: PropTypes.string,
        title: PropTypes.string,
    })),
};

LayoutInput.defaultProps = {
    additionalLabel: undefined,
    changeDataField: undefined,
    focus: false,
    id: undefined,
    maxLength: 0,
    required: false,
    type: 'TEXT',
    validation: {},
    values: [],
};

export default LayoutInput;
