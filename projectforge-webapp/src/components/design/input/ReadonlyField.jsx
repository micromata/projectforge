import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import UncontrolledTooltip from 'reactstrap/lib/UncontrolledTooltip';
import { colorPropType } from '../../../utilities/propTypes';
import TooltipIcon from '../TooltipIcon';
import AdditionalLabel from './AdditionalLabel';
import style from './Input.module.scss';

/**
 * ReadonlyField text (with label and optional tooltip)
 */
function ReadonlyField(
    {
        additionalLabel,
        className,
        color,
        cssClass,
        id,
        label,
        tooltip,
        value,
        ...props
    },
) {
    return (
        <div className={classNames(style.formGroup, 'form-group', className, cssClass)}>
            <div
                id={id}
                className={style.textArea}
                {...props}
            >
                {`${label} ${value}`}
                {tooltip && (
                    <React.Fragment>
                        <TooltipIcon />
                        <UncontrolledTooltip placement="auto" target={id}>
                            {tooltip}
                        </UncontrolledTooltip>
                    </React.Fragment>
                )}
            </div>
            <AdditionalLabel title={additionalLabel} />
        </div>
    );
}

ReadonlyField.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    className: PropTypes.string,
    color: colorPropType,
    cssClass: PropTypes.string,
    tooltip: PropTypes.string,
    value: PropTypes.string,
};

ReadonlyField.defaultProps = {
    additionalLabel: undefined,
    className: undefined,
    color: undefined,
    cssClass: undefined,
    tooltip: undefined,
    value: undefined,
};

export default ReadonlyField;
