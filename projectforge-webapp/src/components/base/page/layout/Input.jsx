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
            case 'checkbox':
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
            'max-length': maxLength,
            required,
            type,
        } = this.props;

        let Tag;
        const properties = {};
        const value = data[id] || '';


        switch (type) {
            case 'input':
                Tag = Input;
                break;
            case 'textarea':
                Tag = TextArea;
                break;
            case 'checkbox':
                Tag = CheckBox;
                properties.checked = value || false;
                break;
            default:
                Tag = 'div';
        }

        if (
            type !== 'checkbox'
            && ((required && value) || (maxLength && value.length > maxLength))
        ) {
            properties.color = 'danger';
        }

        return (
            <Tag
                label={label}
                id={id}
                {...properties}
                onChange={this.handleInputChange}
                value={data[id] || ''}
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
    'max-length': PropTypes.number,
    required: PropTypes.bool,
};

LayoutInput.defaultProps = {
    changeDataField: undefined,
    id: undefined,
    type: 'text',
    values: [],
    'max-length': 0,
    required: false,
};

export default LayoutInput;
