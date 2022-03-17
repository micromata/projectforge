import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import UncontrolledTooltip from 'reactstrap/lib/UncontrolledTooltip';
import { colorPropType } from '../../../utilities/propTypes';
import TooltipIcon from '../TooltipIcon';
import AdditionalLabel from './AdditionalLabel';
import style from './Input.module.scss';

const MAX_ROWS = 10;

function TextArea(
    {
        additionalLabel,
        className,
        color,
        cssClass,
        id,
        label,
        maxRows,
        onChange,
        onKeyDown,
        rows,
        tooltip,
        value,
        ...props
    },
) {
    const [active, setActive] = React.useState(value);
    const [dynamicRows, setDynamicRows] = React.useState(rows);

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

    const handleChange = (event) => {
        if (onChange) {
            onChange(event);
        }

        // Resize TextArea till Max Rows is reached.
        setDynamicRows(Math.min(
            Math.max(
                rows,
                event.target.value.split('\n').length,
            ),
            MAX_ROWS,
        ));
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
                    onBlur={(event) => setActive(event.target.value !== '')}
                    onChange={handleChange}
                    onFocus={() => setActive(true)}
                    onKeyDown={handleKeyDown}
                    rows={dynamicRows}
                    value={value}
                />
                <span className={style.text} id={`textarea-label-${id}`}>
                    {label}
                    {tooltip && <TooltipIcon />}
                </span>
            </label>
            <AdditionalLabel title={additionalLabel} />
            {tooltip && (
                <UncontrolledTooltip placement="auto" target={`textarea-label-${id}`}>
                    {tooltip}
                </UncontrolledTooltip>
            )}
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
    onChange: PropTypes.func,
    onKeyDown: PropTypes.func,
    rows: PropTypes.number,
    tooltip: PropTypes.string,
    value: PropTypes.string,
};

TextArea.defaultProps = {
    additionalLabel: undefined,
    className: undefined,
    color: undefined,
    cssClass: undefined,
    maxRows: undefined,
    onChange: undefined,
    onKeyDown: undefined,
    rows: 3,
    tooltip: undefined,
    value: undefined,
};

export default TextArea;
