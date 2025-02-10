import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { UncontrolledTooltip } from 'reactstrap';
import { colorPropType } from '../../../utilities/propTypes';
import TooltipIcon from '../TooltipIcon';
import AdditionalLabel from './AdditionalLabel';
import style from './Input.module.scss';

function CheckBox(
    {
        additionalLabel,
        className,
        color,
        id,
        label,
        tooltip,
        ...props
    },
) {
    return (
        <>
            <div className={classNames(style.formGroup, className, style.checkboxGroup)}>
                <label
                    className={style.checkboxLabel}
                    htmlFor={id}
                >
                    <input
                        type="checkbox"
                        className={style.checkbox}
                        id={id}
                        {...props}
                    />
                    <span
                        className={classNames(style.text, style[color])}
                        id={`checkbox-label-${String.idify(id)}`}
                    >
                        {label}
                        {tooltip && <TooltipIcon />}
                    </span>
                </label>
                <AdditionalLabel title={additionalLabel} />
            </div>
            {tooltip && (
                <UncontrolledTooltip placement="auto" target={`checkbox-label-${String.idify(id)}`}>
                    {tooltip}
                </UncontrolledTooltip>
            )}
        </>
    );
}

CheckBox.propTypes = {
    id: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    label: PropTypes.string,
    className: PropTypes.string,
    color: colorPropType,
    tooltip: PropTypes.string,
};

export default CheckBox;
