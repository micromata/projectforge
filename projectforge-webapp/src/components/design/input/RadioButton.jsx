import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import UncontrolledTooltip from 'reactstrap/lib/UncontrolledTooltip';
import { colorPropType } from '../../../utilities/propTypes';
import TooltipIcon from '../TooltipIcon';
import AdditionalLabel from './AdditionalLabel';
import style from './Input.module.scss';

function RadioButton(
    {
        additionalLabel,
        className,
        color,
        id,
        name,
        label,
        tooltip,
        ...props
    },
) {
    return (
        <div className={classNames(style.formGroup, className, style.radioButtonGroup)}>
            <label
                className={style.radioButtonLabel}
                htmlFor={id}
            >
                <input
                    type="radio"
                    className={style.radio}
                    id={id}
                    name={name}
                    // eslint-disable-next-line react/jsx-props-no-spreading
                    {...props}
                />
                <span className={classNames(style.text, style[color])} id={`radio-label-${id}`}>
                    {label}
                    {tooltip && <TooltipIcon />}
                </span>
            </label>
            <AdditionalLabel title={additionalLabel} />
            {tooltip && (
                <UncontrolledTooltip placement="auto" target={`radio-label-${id}`}>
                    {tooltip}
                </UncontrolledTooltip>
            )}
        </div>
    );
}

RadioButton.propTypes = {
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    className: PropTypes.string,
    color: colorPropType,
    label: PropTypes.string,
    tooltip: PropTypes.string,
};

RadioButton.defaultProps = {
    additionalLabel: undefined,
    className: undefined,
    color: undefined,
    label: undefined,
    tooltip: undefined,
};

export default RadioButton;
