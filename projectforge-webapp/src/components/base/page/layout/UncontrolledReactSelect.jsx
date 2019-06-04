import PropTypes from 'prop-types';
import React from 'react';
import { dataPropType } from '../../../../utilities/propTypes';
import { getServiceURL, handleHTTPErrors } from '../../../../utilities/rest';
import ReactSelect from './ReactSelect';

class UncontrolledReactSelect extends React.Component {
    static extractDataValue(props) {
        const {
            id,
            values,
            data,
            valueProperty,
            multi,
        } = props;
        let dataValue = Object.getByString(data, id);
        if (!multi && dataValue && values && values.length && values.length > 0) {
            // For react-select it seems to be important, that the current selected element matches
            // its value of the values list.
            const valueOfArray = (typeof dataValue === 'object') ? dataValue[valueProperty] : dataValue;
            dataValue = values.find(it => it[valueProperty] === valueOfArray);
        }
        return dataValue;
    }

    constructor(props) {
        super(props);
        const dataValue = UncontrolledReactSelect.extractDataValue(props);
        this.state = {
            value: dataValue,
        };

        this.onChange = this.onChange.bind(this);
        this.loadOptions = this.loadOptions.bind(this);
    }

    onChange(newValue) {
        this.setState({ value: newValue });
        const { id, changeDataField } = this.props;
        changeDataField(id, newValue);
    }

    loadOptions(inputValue, callback) {
        const { autoCompletion } = this.props;
        fetch(getServiceURL(autoCompletion.url,
            { search: inputValue }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(handleHTTPErrors) // Catch http status codes like 404 etc.
            .then(response => response.json())
            .then((json) => {
                callback(json);
            })
            // TODO CATCH ERROR
            .catch(() => this.setState({}));
    }

    render() {
        const { value } = this.state;
        const {
            autoCompletion,
            ...props
        } = this.props;
        const url = autoCompletion ? autoCompletion.url : undefined;
        return (
            <ReactSelect
                value={value}
                onChange={this.onChange}
                loadOptions={(url && url.length > 0) ? this.loadOptions : undefined}
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
    values: PropTypes.arrayOf(PropTypes.object),
    valueProperty: PropTypes.string,
    labelProperty: PropTypes.string,
    multi: PropTypes.bool,
    required: PropTypes.bool,
    translations: PropTypes.shape({}).isRequired,
    autoCompletion: PropTypes.shape({
        url: PropTypes.string,
    }),
    getOptionLabel: PropTypes.func,
    className: PropTypes.string,
    tooltip: PropTypes.string,
};

UncontrolledReactSelect.defaultProps = {
    additionalLabel: undefined,
    values: undefined,
    valueProperty: 'value',
    labelProperty: 'label',
    multi: false,
    required: false,
    autoCompletion: undefined,
    getOptionLabel: undefined,
    className: undefined,
    tooltip: undefined,
};
export default UncontrolledReactSelect;
