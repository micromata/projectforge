import classNames from 'classnames';
import React from 'react';
import { SystemStatusContext } from '../../../containers/SystemStatusContext';
import { getServiceURL } from '../../../utilities/rest';
import logoTopRight from './logo-top-right.png';
import style from './TopBar.module.scss';

function TopBar() {
    const { logoUrl } = React.useContext(SystemStatusContext);

    return (
        <div className={style.topbar}>
            {logoUrl
                ? (
                    <img
                        className={classNames(style.logo, style.left)}
                        src={getServiceURL(`../rsPublic/${logoUrl}`)}
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

TopBar.propTypes = {};

TopBar.defaultProps = {};

export default TopBar;
