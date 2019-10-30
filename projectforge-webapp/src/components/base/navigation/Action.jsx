import PropTypes from 'prop-types';
import React from 'react';
import { Link } from 'react-router-dom';
import history from '../../../utilities/history';
import { getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import { NavLink } from '../../design';
import MenuBadge from './categories-dropdown/MenuBadge';

class NavigationAction extends React.Component {
    constructor(props) {
        super(props);

        this.handleClick = this.handleClick.bind(this);
    }

    handleClick(event) {
        event.preventDefault();

        const { type, url } = this.props;

        if (type === 'RESTCALL') {
            fetch(
                getServiceURL(url),
                {
                    method: 'GET',
                    credentials: 'include',
                },
            )
                .then(handleHTTPErrors)
                .then(response => response.json())
                .then(({ targetType, url: redirectUrl }) => {
                    switch (targetType) {
                        case 'REDIRECT':
                            history.push(redirectUrl);
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
            title,
            type,
            url,
        } = this.props;
        let content = title;

        if (badge && badge.counter && entryKey) {
            content = (
                <div style={{ position: 'relative' }}>
                    {title}
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

        switch (type) {
            case 'RESTCALL':
                return (
                    <NavLink
                        onClick={this.handleClick}
                        onKeyPress={() => {
                        }}
                    >
                        {content}
                    </NavLink>
                );
            case 'DOWNLOAD':
                return (
                    <NavLink href={getServiceURL(url)} target="_blank" rel="noopener noreferrer">
                        {content}
                    </NavLink>
                );
            case 'LINK':
            case 'REDIRECT':
                return (
                    <NavLink tag={Link} to={`/${url}`}>
                        {content}
                    </NavLink>
                );
            case 'TEXT':
            default:
                return <span className="nav-link">{content}</span>;
        }
    }
}

NavigationAction.propTypes = {
    title: PropTypes.string.isRequired,
    badge: PropTypes.shape({
        counter: PropTypes.number,
    }),
    badgeIsFlying: PropTypes.bool,
    entryKey: PropTypes.string,
    type: PropTypes.oneOf([
        'REDIRECT',
        'RESTCALL',
        'DOWNLOAD',
        'LINK',
        'TEXT',
    ]),
    url: PropTypes.string,
};

NavigationAction.defaultProps = {
    badge: undefined,
    badgeIsFlying: true,
    entryKey: undefined,
    type: 'LINK',
    url: '',
};

export default NavigationAction;
