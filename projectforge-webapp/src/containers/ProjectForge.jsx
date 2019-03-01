import React from 'react';
import { connect } from 'react-redux';
import TopBar from '../components/base/topbar/View';

function ProjectForge() {
    return (
        <React.Fragment>
            <TopBar />
        </React.Fragment>
    );
}

const mapStateToProps = state => ({
    loggedIn: state.authentication.user !== null,
});

export default connect(mapStateToProps)(ProjectForge);
