import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import AdditionalLabel from './AdditionalLabel';
import style from './Input.module.scss';

function Input(
    {
        additionalLabel,
        className,
        color,
        id,
        label,
        type,
        value,
        autoCompletionUrl,
        ...props
    },
) {

    const fetchAutoCompletion = () => {
        const { autoCompletionUrl, value } = this.props;

        fetch(
            getServiceURL(`${autoCompletionUrl}${value}`),
            {
                method: 'GET',
                credentials: 'include',
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(autoCompletion => this.setState({ autoCompletion }))
            .catch(() => this.setState({ autoCompletion: [] }));
    };

    const handleInputChange = (_, newValue) => {
        const {
            id,
            minChars,
            onChange,
            value,
        } = this.props;

        onChange(id, newValue);

        if (value.length < minChars && newValue.length >= minChars) {
            fetchAutoCompletion();
        }
    };

    console.log(autoCompletionUrl)
    // Use new React Hook Feature
    // https://reactjs.org/docs/hooks-intro.html
    const [active, setActive] = React.useState(false);

    return (
        <div className={classNames(style.formGroup, 'form-group', className)}>
            <label
                className={classNames(
                    style.label,
                    { [style.active]: value || active },
                    style[color],
                )}
                htmlFor={id}
            >
                <input
                    className={style.input}
                    type={type}
                    id={id}
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

Input.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    className: PropTypes.string,
    color: colorPropType,
    type: PropTypes.string,
    value: PropTypes.string,
    autoCompletionUrl: PropTypes.string,
};

Input.defaultProps = {
    additionalLabel: undefined,
    className: undefined,
    color: undefined,
    type: 'text',
    value: undefined,
    autoCompletionUrl: undefined,
};

export default Input;
