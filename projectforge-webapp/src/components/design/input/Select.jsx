import { faCheckCircle, faChevronDown } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { selectProps } from '../../../utilities/propTypes';
import AdditionalLabel from './AdditionalLabel';
import style from './Input.module.scss';

class Select extends Component {
    constructor(props) {
        super(props);

        this.state = {
            active: false,
        };

        this.setActive = this.setActive.bind(this);
        this.handleOptionClick = this.handleOptionClick.bind(this);
    }

    setActive(active) {
        this.setState({
            active,
        });
    }

    handleOptionClick(event) {
        const { setSelected } = this.props;
        const { value } = event.target.dataset;

        this.setActive(false);
        setSelected(value);

        event.preventDefault();
        event.stopPropagation();
    }

    render() {
        const {
            additionalLabel,
            id,
            label,
            selected,
        } = this.props;
        let { options } = this.props;
        const { active } = this.state;

        if (typeof options[0] !== 'object') {
            options = options.map(option => ({
                value: option,
                title: option,
            }));
        }

        const value = options.find(option => option.value === selected) || options[0];

        return (
            <div className={style.formGroup}>
                <label
                    htmlFor={id}
                    className={classNames(
                        style.selectLabel,
                        { [style.active]: active },
                    )}
                >
                    <span className={style.text}>{label}</span>
                    <input
                        type="text"
                        id={id}
                        className={style.select}
                        value={value.title}
                        onFocus={() => this.setActive(true)}
                        onBlur={() => this.setActive(false)}
                        readOnly
                    />
                    <FontAwesomeIcon icon={faChevronDown} className={style.icon} />
                    <ul className={style.options}>
                        {options.map(option => (
                            <li
                                key={`select-${id}-option-${option.value}`}
                                className={classNames(
                                    style.option,
                                    { [style.selected]: selected === option.value },
                                )}
                            >
                                <span
                                    onClick={this.handleOptionClick}
                                    onKeyPress={() => {
                                    }}
                                    data-value={option.value}
                                    role="button"
                                    tabIndex={-1}
                                >
                                    {option.title}
                                    {selected === option.value
                                        ? <FontAwesomeIcon icon={faCheckCircle} />
                                        : ''}
                                </span>
                            </li>
                        ))}
                    </ul>
                </label>
                <AdditionalLabel title={additionalLabel} />
            </div>
        );
    }
}

Select.propTypes = {
    ...selectProps,
    selected: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    setSelected: PropTypes.func.isRequired,
};

export default Select;
