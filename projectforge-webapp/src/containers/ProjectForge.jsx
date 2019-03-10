import React, { Component } from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import TopBar from '../components/base/topbar';
import Footer from '../components/base/footer';
import LoginView from '../components/authentication/LoginView';
import { loadSessionIfAvailable, loginUser } from '../actions';

class ProjectForge extends Component {
    componentDidMount() {
        const { loadSessionIfAvailable: loadSession } = this.props;

        loadSession();
    }


    render() {
        const { loggedIn, loginUser: login, loginInProgress } = this.props;
        let content;

        if (loggedIn) {
            content = 'Logged In';
        } else {
            content = (
                <LoginView
                    // TODO: EXAMPLE DATA, REPLACE WITH REAL DATA FROM REST API
                    motd="Please try user demo with password demo123. Have a lot of fun!"
                    login={login}
                    loading={loginInProgress}
                />
            );
        }

        return (
            <React.Fragment>
                <TopBar />
                {content}
                <Footer version="Version 6.25-SNAPSHOT, 2019-02-26" updateAvailable />
            </React.Fragment>
        );
    }
}

ProjectForge.propTypes = {
    loginUser: PropTypes.func.isRequired,
    loggedIn: PropTypes.bool.isRequired,
    loadSessionIfAvailable: PropTypes.func.isRequired,
    loginInProgress: PropTypes.bool.isRequired,
};

const mapStateToProps = state => ({
    loggedIn: state.authentication.user !== null,
    loginInProgress: state.authentication.loading,
});

const actions = {
    loginUser,
    loadSessionIfAvailable,
};

export default connect(mapStateToProps, actions)(ProjectForge);
