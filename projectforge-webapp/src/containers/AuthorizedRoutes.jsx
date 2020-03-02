import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Route, Switch, useLocation } from 'react-router-dom';
import GlobalNavigation from '../components/base/navigation/GlobalNavigation';
import { Alert, Modal, ModalBody } from '../components/design';
import history from '../utilities/history';
import prefix from '../utilities/prefix';
import { getServiceURL } from '../utilities/rest';
import CalendarPage from './page/calendar/CalendarPage';
import DynamicPage from './page/DynamicPage';
import EditPage from './page/edit/EditPage';
import IndexPage from './page/IndexPage';
import ListPage from './page/list/ListPage';
import TaskTreePage from './page/TaskTreePage';

export const wicketRoute = (
    <Route
        path="/wa"
        component={({ location }) => {
            if (process.env.NODE_ENV === 'development') {
                return (
                    <a href={getServiceURL(`..${location.pathname}`)}>
                        Redirect to Wicket
                    </a>
                );
            }

            window.location.reload();
            return <React.Fragment />;
        }}
    />
);

function AuthorizedRoutes(
    {
        alertMessage,
    },
) {
    const location = useLocation();

    const { background } = location.state || {};

    const routes = switchLocation => (
        <Switch location={switchLocation}>
            {wicketRoute}
            <Route
                exact
                path={prefix}
                component={IndexPage}
            />
            <Route
                path={`${prefix}calendar`}
                component={CalendarPage}
            />
            <Route
                path={`${prefix}taskTree`}
                component={TaskTreePage}
            />
            <Route
                path={`${prefix}dynamic/:page`}
                component={DynamicPage}
            />
            <Route
                path={`${prefix}:category/edit/:id?`}
                component={EditPage}
            />
            <Route
                path={`${prefix}:category`}
                component={ListPage}
            />
        </Switch>
    );

    return (
        <React.Fragment>
            <GlobalNavigation />
            {alertMessage ? (
                <Alert color="danger">
                    {alertMessage}
                </Alert>
            ) : undefined}
            {routes(background || location)}
            {background && (
                <Modal
                    size="xl"
                    isOpen
                    toggle={() => history.push(background.pathname, { background: undefined })}
                >
                    <ModalBody>
                        {routes(location)}
                    </ModalBody>
                </Modal>
            )}
        </React.Fragment>
    );
}

AuthorizedRoutes.propTypes = {
    alertMessage: PropTypes.string,
};

AuthorizedRoutes.defaultProps = {
    alertMessage: undefined,
};

const mapStateToProps = ({ authentication }) => ({
    alertMessage: authentication.alertMesage,
});

export default connect(mapStateToProps)(AuthorizedRoutes);
