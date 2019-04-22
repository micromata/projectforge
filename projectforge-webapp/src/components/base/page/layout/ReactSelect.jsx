import React from 'react';
import Select from 'react-select';
import AsyncSelect from 'react-select/lib/Async';
import makeAnimated from 'react-select/lib/animated';
import PropTypes from 'prop-types';
import { dataPropType } from '../../../../utilities/propTypes';
import style from '../../../design/input/Input.module.scss';
import AdditionalLabel from '../../../design/input/AdditionalLabel';

class ReactSelect extends React.Component {
    constructor(props) {
        super(props);
        this.state = { value: undefined };

        this.setSelected = this.setSelected.bind(this);
    }

    componentDidMount() {
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
        this.setState({ value: dataValue });
    }

    setSelected(newValue) {
        this.setState({ value: newValue });
        const { id, changeDataField } = this.props;
        changeDataField(id, newValue);
    }

    render() {
        const { value } = this.state;
        const {
            label,
            additionalLabel,
            values,
            isMulti,
            isRequired,
            valueProperty,
            labelProperty,
            translations,
            loadOptions,
            getOptionLabel,
            className,
        } = this.props;
        let Tag = Select;
        let defaultOptions;
        if (loadOptions) {
            Tag = AsyncSelect;
            defaultOptions = true;
        }
        return (
            <React.Fragment>
                <span className={style.text}>{label}</span>
                <Tag
                    // closeMenuOnSelect={false}
                    components={makeAnimated()}
                    value={value}
                    isMulti={isMulti}
                    options={values}
                    isClearable={!isRequired}
                    getOptionValue={option => (option[valueProperty])}
                    getOptionLabel={getOptionLabel || (option => (option[labelProperty]))}
                    onChange={this.setSelected}
                    loadOptions={loadOptions}
                    defaultOptions={defaultOptions}
                    placeholder={translations['select.placeholder']}
                    className={className}
                    cache={{}}
                />
                <AdditionalLabel title={additionalLabel}/>
            </React.Fragment>
        );
    }
}

ReactSelect.propTypes = {
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

ReactSelect.defaultProps = {
    additionalLabel: undefined,
    valueProperty: 'value',
    labelProperty: 'label',
    isMulti: false,
    isRequired: false,
    loadOptions: undefined,
    getOptionLabel: undefined,
    className: undefined,
};
export default ReactSelect;
