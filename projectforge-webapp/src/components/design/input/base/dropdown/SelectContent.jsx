import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import style from '../Base.module.scss';

function DropdownSelectContent(
    {
        select,
        selectIndex,
        setSelected,
        values,
    },
) {
    return (
        <ul className={style.selectContent}>
            {values.map((option, index) => (
                <li
                    className={classNames(
                        style.selectOption,
                        { [style.keySelected]: selectIndex === index },
                    )}
                    key={`dropdown-select-option-${option}`}
                >
                    <span
                        onMouseOver={() => setSelected(index)}
                        onMouseLeave={() => setSelected(-1)}
                        onClick={({ target }) => select(index, target)}
                        onFocus={() => {
                        }}
                        onKeyPress={() => {
                        }}
                        role="button"
                        tabIndex={-1}
                    >
                        {option}
                    </span>
                </li>
            ))}
        </ul>
    );
}

DropdownSelectContent.propTypes = {
    select: PropTypes.func.isRequired,
    setSelected: PropTypes.func.isRequired,
    selectIndex: PropTypes.number,
    values: PropTypes.arrayOf(PropTypes.string),
};

DropdownSelectContent.defaultProps = {
    selectIndex: -1,
    values: [],
};

export default DropdownSelectContent;
