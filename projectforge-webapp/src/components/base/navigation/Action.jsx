import PropTypes from 'prop-types';
import React from 'react';
import { Link } from 'react-router-dom';
import history from '../../../utilities/history';
import { getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import { NavLink } from '../../design';

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
        const { type, title, url } = this.props;

        switch (type) {
            case 'RESTCALL':
                return (
                    <NavLink
                        onClick={this.handleClick}
                        onKeyPress={() => {
                        }}
                    >
                        {title}
                    </NavLink>
                );
            case 'DOWNLOAD':
                return (
                    <NavLink href={getServiceURL(url)} target="_blank" rel="noopener noreferrer">
                        {title}
                    </NavLink>
                );
            case 'LINK':
            case 'REDIRECT':
                return (
                    <NavLink tag={Link} to={`/${url}`}>
                        {title}
                    </NavLink>
                );
            case 'TEXT':
            default:
                return <span className="nav-link">{title}</span>;
        }
    }
}

NavigationAction.propTypes = {
    title: PropTypes.string.isRequired,
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
    type: 'LINK',
    url: '',
};

export default NavigationAction;
