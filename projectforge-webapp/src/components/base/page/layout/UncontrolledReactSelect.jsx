import React from 'react';
import PropTypes from 'prop-types';
import { dataPropType } from '../../../../utilities/propTypes';
import ReactSelect from './ReactSelect';

class UncontrolledReactSelect extends React.Component {
    constructor(props) {
        super(props);
        const {
            id,
            values,
            data,
            valueProperty,
        } = this.props;
        let dataValue = Object.getByString(data, id);
        if (dataValue && values && values.length && values.length > 0) {
            // For react-select it seems to be important, that the current selected element matches
            // its value of the values list.
            const valueOfArray = (typeof dataValue === 'object') ? dataValue[valueProperty] : dataValue;
            dataValue = values.find(it => it[valueProperty] === valueOfArray);
        }
        this.state = { value: dataValue };

        this.onChange = this.onChange.bind(this);
    }

    onChange(newValue) {
        this.setState({ value: newValue });
        const { id, changeDataField } = this.props;
        changeDataField(id, newValue);
    }

    render() {
        const { value } = this.state;
        const {
            ...props
        } = this.props;
        return (
            <ReactSelect
                value={value}
                onChange={this.onChange}
                {...props}
            />
        );
    }
}

UncontrolledReactSelect.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    data: dataPropType.isRequired,
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    values: PropTypes.arrayOf(PropTypes.object).isRequired,
    valueProperty: PropTypes.string,
    labelProperty: PropTypes.string,
    isMulti: PropTypes.bool,
    isRequired: PropTypes.bool,
    translations: PropTypes.shape({}).isRequired,
    loadOptions: PropTypes.func,
    getOptionLabel: PropTypes.func,
    className: PropTypes.string,
};

UncontrolledReactSelect.defaultProps = {
    additionalLabel: undefined,
    valueProperty: 'value',
    labelProperty: 'label',
    isMulti: false,
    isRequired: false,
    loadOptions: undefined,
    getOptionLabel: undefined,
    className: undefined,
};
export default UncontrolledReactSelect;
