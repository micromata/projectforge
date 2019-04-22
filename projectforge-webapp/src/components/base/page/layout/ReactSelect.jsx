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

        this.setSelected = this.setSelected.bind(this);
    }

    setSelected(newValue) {
        const { id, changeDataField } = this.props;
        changeDataField(id, newValue);
    }

    render() {
        const {
            label,
            additionalLabel,
            id,
            values,
            data,
            isMulti,
            isRequired,
            valueProperty,
            labelProperty,
            translations,
            loadOptions,
            getOptionLabel,
            className,
        } = this.props;
        let defaultValue = Object.getByString(data, id);
        if (defaultValue && values && values.length) {
            const value = (typeof defaultValue === 'object') ? defaultValue[valueProperty] : defaultValue;
            defaultValue = values.find(it => it[valueProperty] === value);
        }
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
                    defaultValue={defaultValue}
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
                />
                <AdditionalLabel title={additionalLabel} />
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
