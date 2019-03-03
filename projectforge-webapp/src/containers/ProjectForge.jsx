import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import TopBar from '../components/base/topbar';
import Footer from '../components/base/footer';
import LoginView from '../components/authentication/LoginView';

function ProjectForge({ loggedIn }) {
    let content;

    if (loggedIn) {
        content = 'Logged In';
    } else {
        content = (
            <LoginView
                // TODO: EXAMPLE DATA, REPLACE WITH REAL DATA FROM REST API
                motd="Please try user demo with password demo123. Have a lot of fun!"
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
    loggedIn: PropTypes.bool,
};

ProjectForge.defaultProps = {
    loggedIn: false,
};

const mapStateToProps = state => ({
    loggedIn: state.authentication.user !== null,
});

export default connect(mapStateToProps)(ProjectForge);
