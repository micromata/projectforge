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
        cssClass,
        id,
        label,
        maxRows,
        onKeyDown,
        value,
        ...props
    },
) {
    const [active, setActive] = React.useState(value);

    const handleKeyDown = (event) => {
        if (onKeyDown) {
            onKeyDown(event);
        }

        if (event.ctrlKey && event.key === 'Enter') {
            for (let i = 0; i < document.forms.length; i += 1) {
                // check all forms if textarea is child
                if (document.forms[i].querySelector(`#${id}`)) {
                    // find submit button of form and click it.
                    document.forms[i].querySelector('button[type="submit"]')
                        .click();
                    break;
                }
            }
        }
    };

    return (
        <div className={classNames(style.formGroup, 'form-group', className, cssClass)}>
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
                    onKeyDown={handleKeyDown}
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
    cssClass: PropTypes.string,
    maxRows: PropTypes.number,
    onKeyDown: PropTypes.func,
    value: PropTypes.string,
};

TextArea.defaultProps = {
    additionalLabel: undefined,
    className: undefined,
    color: undefined,
    cssClass: undefined,
    maxRows: undefined,
    onKeyDown: undefined,
    value: undefined,
};

export default TextArea;
