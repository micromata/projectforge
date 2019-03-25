import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import style from './Input.module.scss';

function TextArea(
    {
        className,
        color,
        id,
        label,
        ...props
    },
) {
    const [active, setActive] = React.useState(false);

    return (
        <div className={classNames(style.formGroup, className)}>
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
                />
                <span className={style.text}>{label}</span>
            </label>
        </div>
    );
}

TextArea.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    className: PropTypes.string,
    color: colorPropType,
};

TextArea.defaultProps = {
    className: undefined,
    color: undefined,
};

export default TextArea;
