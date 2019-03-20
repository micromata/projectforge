import PropTypes from 'prop-types';
import React, { Component } from 'react';
import revisedRandomId from '../../../../utilities/revisedRandomId';
import { Col, Input } from '../../../design';
import style from '../Page.module.scss';

class LayoutInput extends Component {
    constructor(props) {
        super(props);

        this.handleInputChange = this.handleInputChange.bind(this);
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

    render() {
        const {
            id,
            type,
            values,
            data,
            'max-length': maxLength,
            required,
        } = this.props;

        // TODO: VALIDATION

        let children;
        let ColTag = Col;
        const inputProps = {};

        if (type === 'input') {
            inputProps.type = 'text';
        }

        if (type === 'select') {
            children = values.map(option => (
                <option
                    value={option.value}
                    key={`input-select-value-${revisedRandomId()}`}
                >
                    {option.title}
                </option>
            ));

            ColTag = React.Fragment;
            inputProps.className = style.select;
        }

        if (type === 'checkbox') {
            inputProps.checked = data[id] || false;
        } else {
            inputProps.value = data[id] || '';
        }

        if (type !== 'checkbox' && type !== 'select') {
            if (required && !inputProps.value) {
                inputProps.invalid = true;
            }

            if (maxLength && inputProps.value.length > maxLength) {
                inputProps.invalid = true;
            }
        }

        return (
            <ColTag>
                <Input
                    type={type}
                    name={id}
                    id={id}
                    {...inputProps}
                    onChange={this.handleInputChange}
                >
                    {children}
                </Input>
            </ColTag>
        );
    }
}

LayoutInput.propTypes = {
    changeDataField: PropTypes.func,
    data: PropTypes.objectOf(PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number,
        PropTypes.bool,
        PropTypes.objectOf(PropTypes.oneOfType([
            PropTypes.string,
            PropTypes.number,
            PropTypes.bool,
        ])),
    ])).isRequired,
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
