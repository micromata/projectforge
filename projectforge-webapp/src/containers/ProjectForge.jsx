import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import TopBar from '../components/base/topbar';
import Footer from '../components/base/footer';
import LoginView from '../components/authentication/LoginView';
import { loginUser } from '../actions';

function ProjectForge({ loggedIn, loginUser: login }) {
    let content;

    if (loggedIn) {
        content = 'Logged In';
    } else {
        content = (
            <LoginView
                // TODO: EXAMPLE DATA, REPLACE WITH REAL DATA FROM REST API
                motd="Please try user demo with password demo123. Have a lot of fun!"
                login={login}
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

ProjectForge.propTypes = {
    loginUser: PropTypes.func.isRequired,
    loggedIn: PropTypes.bool.isRequired,
};

const mapStateToProps = state => ({
    loggedIn: state.authentication.user !== null,
});

const actions = {
    loginUser,
};

export default connect(mapStateToProps, actions)(ProjectForge);
