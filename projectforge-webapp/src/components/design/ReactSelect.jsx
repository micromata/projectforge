import { faQuestion } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import Select from 'react-select';
import makeAnimated from 'react-select/animated';
import Async from 'react-select/async';
import AsyncCreatable from 'react-select/async-creatable';
import { UncontrolledTooltip } from 'reactstrap';
import AdditionalLabel from './input/AdditionalLabel';
import style from './input/Input.module.scss';

function ReactSelect(
    {
        additionalLabel,
        autoCompletion,
        getOptionLabel,
        id,
        label,
        labelProperty,
        loadOptions,
        multi,
        required,
        tooltip,
        translations,
        value,
        valueProperty,
        values,
        ...props
    },
) {
    let Tag = Select;
    let defaultOptions;
    let options;

    if (loadOptions) {
        if (autoCompletion && autoCompletion.type !== undefined) {
            Tag = Async;
        } else {
            Tag = AsyncCreatable;
        }
        if (values && values.length > 0) {
            // values are now the default options for the drop down without autocompletion call.
            defaultOptions = values;
        } else {
            defaultOptions = true;
        }
    } else {
        options = values;
    }

    let tooltipElement;
    if (tooltip && id) {
        const tooltipId = `rs-tooltip-${id}`;
        tooltipElement = (
            <React.Fragment>
                <span>{' '}</span>
                <FontAwesomeIcon
                    icon={faQuestion}
                    className={style.icon}
                    size="sm"
                    id={tooltipId}
                    style={{ color: 'gold' }}
                />
                <UncontrolledTooltip placement="right" target={tooltipId}>
                    {tooltip}
                </UncontrolledTooltip>
            </React.Fragment>
        );
    }

    const getOptionLabelDefault = (option) => {
        // Property __isNew__ is provided by react select and can't be renamed.
        // eslint-disable-next-line no-underscore-dangle
        if (option.__isNew__) {
            return option.label;
        }

        if (!option) {
            return '';
        }

        return option[labelProperty];
    };

    return (
        <React.Fragment>
            <span className={`mm-select ${style.text}`}>{label}</span>
            {tooltipElement}
            <Tag
                // closeMenuOnSelect={false}
                components={makeAnimated()}
                options={options}
                isClearable={!required}
                getOptionValue={option => (option[valueProperty])}
                getOptionLabel={getOptionLabel || getOptionLabelDefault}
                loadOptions={loadOptions}
                defaultOptions={defaultOptions}
                id={id}
                isMulti={multi}
                placeholder={translations['select.placeholder'] || ''}
                cache={{}}
                value={value || null}
                {...props}
            />
            <AdditionalLabel title={additionalLabel} />
        </React.Fragment>
    );
}

ReactSelect.propTypes = {
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    autoCompletion: PropTypes.shape({
        type: PropTypes.oneOf(['USER', 'GROUP', undefined]),
    }),
    value: PropTypes.oneOfType([
        PropTypes.shape({}),
        PropTypes.arrayOf(PropTypes.shape({})),
    ]),
    defaultValue: PropTypes.oneOfType([
        PropTypes.shape({}),
        PropTypes.arrayOf(PropTypes.shape({})),
    ]),
    id: PropTypes.string,
    values: PropTypes.arrayOf(PropTypes.object),
    valueProperty: PropTypes.string,
    labelProperty: PropTypes.string,
    multi: PropTypes.bool,
    required: PropTypes.bool,
    translations: PropTypes.shape({}).isRequired,
    loadOptions: PropTypes.func,
    getOptionLabel: PropTypes.func,
    onChange: PropTypes.func,
    className: PropTypes.string,
    tooltip: PropTypes.string,
};

ReactSelect.defaultProps = {
    autoCompletion: undefined,
    id: undefined,
    values: undefined,
    value: undefined,
    defaultValue: undefined,
    additionalLabel: undefined,
    valueProperty: 'value',
    labelProperty: 'label',
    multi: false,
    required: false,
    loadOptions: undefined,
    getOptionLabel: undefined,
    onChange: undefined,
    className: undefined,
    tooltip: undefined,
};
export default ReactSelect;
