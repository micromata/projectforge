import { faQuestion } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import Creatable from 'react-select/creatable';
import { UncontrolledTooltip } from 'reactstrap';
import style from '../input/Input.module.scss';
import ReactSelectControlWithLabel from './ReactSelectControlWithLabel';

function ReactCreatableSelect(
    {
        additionalLabel,
        className,
        color,
        id,
        label,
        required,
        tooltip,
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        translations,
        values,
        ...props
    },
) {
    const selectRef = React.useRef(null);

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

    return (
        <div className="react-select">
            {tooltipElement}
            <Creatable
                cache={{}}
                className={classNames(
                    className,
                    'react-select__container',
                    { hasValue: Boolean(values) },
                    color,
                )}
                classNamePrefix="react-select"
                components={{ Control: ReactSelectControlWithLabel }}
                id={id}
                isClearable={!required}
                isMulti
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
                label={label}
                values={values}
                {...props}
            />
            {additionalLabel && (
                <span className="react-select__additional-label">{additionalLabel}</span>
            )}
        </div>
    );
}

ReactCreatableSelect.propTypes = {
    label: PropTypes.string.isRequired,
    translations: PropTypes.shape({}).isRequired,
    additionalLabel: PropTypes.string,
    autoCompletion: PropTypes.shape({
        type: PropTypes.oneOf(['USER', 'GROUP', 'EMPLOYEE', undefined]),
    }),
    className: PropTypes.string,
    color: PropTypes.string,
    id: PropTypes.string,
    onChange: PropTypes.func,
    required: PropTypes.bool,
    tooltip: PropTypes.string,
    values: PropTypes.arrayOf(PropTypes.string),
};

ReactCreatableSelect.defaultProps = {
    additionalLabel: undefined,
    autoCompletion: undefined,
    className: undefined,
    color: undefined,
    id: undefined,
    onChange: undefined,
    required: false,
    tooltip: undefined,
    values: undefined,
};
export default ReactCreatableSelect;
