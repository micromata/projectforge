import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import AdditionalLabel from './AdditionalLabel';
import style from './Input.module.scss';

function TextArea(
    {
        additionalLabel,
        className,
        color,
        id,
        label,
        value,
        ...props
    },
) {
    const [active, setActive] = React.useState(value);

    return (
        <div className={classNames(style.formGroup, 'form-group', className)}>
            <label
                className={classNames(
                    style.textAreaLabel,
                    { [style.active]: active },
                    style[color],
                )}
                htmlFor={id}
            >
                <textarea
                    id={id}
                    className={style.textArea}
                    {...props}
                    onFocus={() => setActive(true)}
                    onBlur={event => setActive(event.target.value !== '')}
                    value={value}
                />
                <span className={style.text}>{label}</span>
            </label>
            <AdditionalLabel title={additionalLabel} />
        </div>
    );
}

TextArea.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    className: PropTypes.string,
    color: colorPropType,
    value: PropTypes.string,
};

TextArea.defaultProps = {
    additionalLabel: undefined,
    className: undefined,
    color: undefined,
    value: undefined,
};

export default TextArea;
