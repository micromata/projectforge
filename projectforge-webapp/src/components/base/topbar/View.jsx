import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import logoTopRight from './logo-top-right.png';
import style from './TopBar.module.scss';

function TopBar({ logo }) {
    return (
        <div className={style.topbar}>
            {logo
                ? (
                    <img
                        className={classNames(style.logo, style.left)}
                        src={logo}
                        alt="Company Logo"
                    />
                )
                : undefined}
            <img
                className={classNames(style.logo, style.right)}
                src={logoTopRight}
                alt="ProjectForge Logo"
            />
        </div>
    );
}

TopBar.propTypes = {
    logo: PropTypes.string,
};

TopBar.defaultProps = {
    logo: null,
};

export default TopBar;
