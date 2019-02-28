import React from 'react';
import {connect} from 'react-redux';

function ProjectForge({loggedIn}) {
    if (!loggedIn) {

    }
}

const mapStateToProps = state => ({
    loggedIn: state.authentication.user !== null
});

export default connect(mapStateToProps)(ProjectForge);
