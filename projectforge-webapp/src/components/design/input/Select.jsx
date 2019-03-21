import { faCheckCircle, faChevronDown } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { uncontrolledSelectProps } from '../../../utilities/propTypes';
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
            id,
            label,
            options,
            selected,
        } = this.props;
        const { active } = this.state;

        const value = options.find(option => option.value === selected);

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
            </div>
        );
    }
}

Select.propTypes = {
    ...uncontrolledSelectProps,
    selected: PropTypes.string.isRequired,
    setSelected: PropTypes.func.isRequired,
};

export default Select;
