import { faQuestion } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import Select from 'react-select';
import Async from 'react-select/async';
import AsyncCreatable from 'react-select/async-creatable';
import { UncontrolledTooltip } from 'reactstrap';
import style from '../input/Input.module.scss';
import ReactSelectControlWithLabel from './ReactSelectControlWithLabel';

function ReactSelect(
    {
        additionalLabel,
        autoCompletion,
        className,
        color,
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
    const selectRef = React.useRef(null);

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
            <>
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
            </>
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
        <div className="react-select">
            {tooltipElement}
            <Tag
                cache={{}}
                className={classNames(
                    className,
                    'react-select__container',
                    { hasValue: Boolean(value) },
                    color,
                )}
                classNamePrefix="react-select"
                components={{ Control: ReactSelectControlWithLabel }}
                defaultOptions={defaultOptions}
                getOptionLabel={getOptionLabel || getOptionLabelDefault}
                getOptionValue={(option) => (option[valueProperty])}
                id={id}
                isClearable={!required}
                isMulti={multi}
                label={label}
                loadOptions={loadOptions}
                options={options}
                ref={selectRef}
                selectRef={selectRef}
                styles={{
                    // Input font size has to be set here, so the component can calculate with
                    // this size.
                    input: (provided) => ({
                        ...provided,
                        fontSize: 15,
                    }),
                }}
                placeholder=""
                value={value || null}
                {...props}
            />
            {additionalLabel && (
                <span className="react-select__additional-label">{additionalLabel}</span>
            )}
        </div>
    );
}

ReactSelect.propTypes = {
    label: PropTypes.string.isRequired,
    translations: PropTypes.shape({}).isRequired,
    additionalLabel: PropTypes.string,
    autoCompletion: PropTypes.shape({
        type: PropTypes.oneOf(['USER', 'GROUP', 'EMPLOYEE', undefined]),
    }),
    className: PropTypes.string,
    color: PropTypes.string,
    defaultValue: PropTypes.oneOfType([
        PropTypes.shape({}),
        PropTypes.arrayOf(PropTypes.shape({})),
    ]),
    getOptionLabel: PropTypes.func,
    id: PropTypes.string,
    labelProperty: PropTypes.string,
    loadOptions: PropTypes.func,
    multi: PropTypes.bool,
    onChange: PropTypes.func,
    required: PropTypes.bool,
    tooltip: PropTypes.string,
    value: PropTypes.oneOfType([
        PropTypes.shape({}),
        PropTypes.arrayOf(PropTypes.shape({})),
    ]),
    valueProperty: PropTypes.string,
    values: PropTypes.arrayOf(PropTypes.object),
};

ReactSelect.defaultProps = {
    additionalLabel: undefined,
    autoCompletion: undefined,
    className: undefined,
    color: undefined,
    defaultValue: undefined,
    getOptionLabel: undefined,
    id: undefined,
    labelProperty: 'label',
    loadOptions: undefined,
    multi: false,
    onChange: undefined,
    required: false,
    tooltip: undefined,
    value: undefined,
    valueProperty: 'value',
    values: undefined,
};
export default ReactSelect;
