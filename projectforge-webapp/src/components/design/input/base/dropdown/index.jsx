import PropTypes from 'prop-types';
import React from 'react';
import style from '../Base.module.scss';

function BaseDropdown({ children }) {
    return (
        <div className={style.dropdown}>
            {children}
        </div>
    );
}

BaseDropdown.propTypes = {
    children: PropTypes.node.isRequired,
};

export default BaseDropdown;
