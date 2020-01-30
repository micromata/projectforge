import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import AdditionalLabel from './AdditionalLabel';
import style from './Input.module.scss';

/**
 * ReadonlyField text (with label and optional toolip)
 */
function ReadonlyField(
    {
        additionalLabel,
        className,
        color,
        cssClass,
        id,
        label,
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
    value: PropTypes.string,
};

ReadonlyField.defaultProps = {
    additionalLabel: undefined,
    className: undefined,
    color: undefined,
    cssClass: undefined,
    value: undefined,
};

export default ReadonlyField;
