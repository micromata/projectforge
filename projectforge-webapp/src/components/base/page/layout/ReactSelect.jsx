import React from 'react';
import Select from 'react-select';
import AsyncSelect from 'react-select/lib/Async';
import makeAnimated from 'react-select/lib/animated';
import PropTypes from 'prop-types';
import style from '../../../design/input/Input.module.scss';
import AdditionalLabel from '../../../design/input/AdditionalLabel';

function ReactSelect(
    {
        label,
        additionalLabel,
        value,
        values,
        isMulti,
        isRequired,
        valueProperty,
        labelProperty,
        translations,
        loadOptions,
        getOptionLabel,
        onChange,
        className,
    },
) {
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
                onChange={onChange}
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

ReactSelect.propTypes = {
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    value: PropTypes.any,
    values: PropTypes.arrayOf(PropTypes.object).isRequired,
    valueProperty: PropTypes.string,
    labelProperty: PropTypes.string,
    isMulti: PropTypes.bool,
    isRequired: PropTypes.bool,
    translations: PropTypes.shape({}).isRequired,
    loadOptions: PropTypes.func,
    getOptionLabel: PropTypes.func,
    onChange: PropTypes.func,
    className: PropTypes.string,
};

ReactSelect.defaultProps = {
    value: undefined,
    additionalLabel: undefined,
    valueProperty: 'value',
    labelProperty: 'label',
    isMulti: false,
    isRequired: false,
    loadOptions: undefined,
    getOptionLabel: undefined,
    onChange: undefined,
    className: undefined,
};
export default ReactSelect;
