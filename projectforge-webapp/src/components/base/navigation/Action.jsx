import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';
import { performGetCall } from '../../../actions';
import { getServiceURL } from '../../../utilities/rest';
import { NavLink } from '../../design';

class NavigationAction extends React.Component {
    constructor(props) {
        super(props);

        this.handleClick = this.handleClick.bind(this);
    }

    handleClick(event) {
        event.preventDefault();

        const { type, getCall, url } = this.props;

        if (type === 'RESTCALL' && getCall) {
            getCall(url);
        }
    }

    render() {
        const { type, title, url } = this.props;

        // TODO: IMPLEMENT REDIRECT
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
    getCall: PropTypes.func.isRequired,
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

const mapStateToProps = () => ({});

const actions = {
    getCall: performGetCall,
};

export default connect(mapStateToProps, actions)(NavigationAction);
