/* eslint-disable no-alert */
import { faHistory } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { useNavigate, useLocation } from 'react-router';
import { loadUserStatus } from '../../../actions';
import history from '../../../utilities/history';
import { getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import { NavLink, UncontrolledTooltip } from '../../design';
import MenuBadge from './categories-dropdown/MenuBadge';

function NavigationAction({
    badge,
    badgeIsFlying = true,
    entryKey,
    id,
    loadUserStatus: checkLogin,
    title,
    tooltip,
    type = 'LINK',
    url = '',
}) {
    const navigate = useNavigate();
    const location = useLocation();
    const handleClick = (event) => {
        event.preventDefault();

        if (type === 'RESTCALL') {
            fetch(
                getServiceURL(url),
                {
                    method: 'GET',
                    credentials: 'include',
                },
            )
                .then(handleHTTPErrors)
                .then((response) => response.json())
                .then(({ targetType, url: redirectUrl }) => {
                    switch (targetType) {
                        case 'REDIRECT':
                            history.push(redirectUrl);
                            break;
                        case 'RELOAD':
                            window.location.reload();
                            break;
                        case 'CHECK_AUTHENTICATION':
                            checkLogin();

                            if (redirectUrl) {
                                history.push(redirectUrl);
                            }
                            break;
                        default:
                            // TODO: HANDLE TOAST MESSAGE
                            alert(`Target type ${targetType} not handled.`);
                    }
                })
                // TODO: HANDLE ERRORS
                .catch(alert);
        }
    };

    let displayTitle = title;
    if (id === 'CLASSIC') {
        displayTitle = <FontAwesomeIcon icon={faHistory} />;
    }
    let content = displayTitle;

    if (badge && badge.counter && entryKey) {
        content = (
            <div style={{ position: 'relative' }}>
                {displayTitle}
                <MenuBadge
                    elementKey={entryKey}
                    color="danger"
                    isFlying={badgeIsFlying}
                    style={{ right: '-1.3em' }}
                >
                    {badge.counter}
                </MenuBadge>
            </div>
        );
    }

    const tooltipElement = tooltip && (
        <UncontrolledTooltip placement="left" target={id}>
            {tooltip}
        </UncontrolledTooltip>
    );

    switch (type) {
        case 'RESTCALL':
            return (
                <>
                    <NavLink
                        id={id}
                        onClick={handleClick}
                        onKeyPress={() => undefined}
                    >
                        {content}
                    </NavLink>
                    {tooltipElement}
                </>
            );
        case 'DOWNLOAD':
            return (
                <>
                    <NavLink
                        id={id}
                        href={getServiceURL(url)}
                        target="_blank"
                        rel="noopener noreferrer"
                    >
                        {content}
                    </NavLink>
                    {tooltipElement}
                </>
            );
        case 'LINK':
        case 'MODAL':
        case 'REDIRECT': {
            const pathname = `/${url.split('?')[0]}`;
            let search = url.split('?')[1] || '';

            // Add modal=true query parameter for MODAL type
            if (type === 'MODAL') {
                search = search ? `${search}&modal=true` : 'modal=true';
            }

            const fullPath = `${pathname}${search ? `?${search}` : ''}`;
            // If we're already in a modal, pass through the original background
            // Otherwise, use current location as background
            const backgroundLocation = location.state?.background || location;
            const navigationOptions = type === 'MODAL' ? { state: { background: backgroundLocation } } : {};

            const handleNavClick = (e) => {
                e.preventDefault();
                navigate(fullPath, navigationOptions);
            };

            return (
                <>
                    <NavLink
                        id={id}
                        href={fullPath}
                        onClick={handleNavClick}
                    >
                        {content}
                    </NavLink>
                    {tooltipElement}
                </>
            );
        }
        case 'TEXT':
        default:
            return (
                <span className="nav-link" id={id}>
                    {content}
                    {tooltipElement}
                </span>
            );
    }
}

NavigationAction.propTypes = {
    loadUserStatus: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
    badge: PropTypes.shape({
        counter: PropTypes.number,
    }),
    badgeIsFlying: PropTypes.bool,
    entryKey: PropTypes.string,
    id: PropTypes.string,
    tooltip: PropTypes.string,
    type: PropTypes.oneOf([
        'DOWNLOAD',
        'LINK',
        'MODAL',
        'REDIRECT',
        'RESTCALL',
        'TEXT',
    ]),
    url: PropTypes.string,
};

const actions = { loadUserStatus };

export default connect(undefined, actions)(NavigationAction);
