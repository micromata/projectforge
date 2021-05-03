import { faHistory } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';
import { loadUserStatus } from '../../../actions';
import history from '../../../utilities/history';
import { getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import { NavLink, UncontrolledTooltip } from '../../design';
import MenuBadge from './categories-dropdown/MenuBadge';

class NavigationAction extends React.Component {
    constructor(props) {
        super(props);

        this.handleClick = this.handleClick.bind(this);
    }

    handleClick(event) {
        event.preventDefault();

        const { type, url, loadUserStatus: checkLogin } = this.props;

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
    }

    render() {
        const {
            badge,
            badgeIsFlying,
            entryKey,
            id,
            title,
            tooltip,
            type,
            url,
        } = this.props;
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
        let tooltipElement;
        if (tooltip) {
            tooltipElement = (
                <UncontrolledTooltip placement="left" target={id}>
                    {tooltip}
                </UncontrolledTooltip>
            );
        }

        switch (type) {
            case 'RESTCALL':
                return (
                    <>
                        <NavLink
                            id={id}
                            onClick={this.handleClick}
                            onKeyPress={() => {
                            }}
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
            case 'REDIRECT':
                return (
                    <>
                        <NavLink
                            id={id}
                            tag={Link}
                            to={{
                                pathname: `/${url}`,
                                state: {
                                    background: type === 'MODAL' ? history.location : undefined,
                                },
                            }}
                        >
                            {content}
                        </NavLink>
                        {tooltipElement}
                    </>
                );
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

NavigationAction.defaultProps = {
    badge: undefined,
    badgeIsFlying: true,
    entryKey: undefined,
    id: undefined,
    tooltip: undefined,
    type: 'LINK',
    url: '',
};

const actions = { loadUserStatus };

export default connect(undefined, actions)(NavigationAction);
