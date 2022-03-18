import Helmet from 'react-helmet';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Route, Switch } from 'react-router-dom';
import GlobalNavigation from '../components/base/navigation/GlobalNavigation';
import { Alert, Container } from '../components/design';
import prefix from '../utilities/prefix';
import { getServiceURL } from '../utilities/rest';
import CalendarPage from './page/calendar/CalendarPage';
import FormPage from './page/form/FormPage';
import IndexPage from './page/IndexPage';
import ListPage from './page/list/ListPage';
import TaskTreePage from './page/TaskTreePage';
import ModalRoutes from './ModalRoutes';

export const wicketRoute = (
    <Route
        path="/wa"
        component={({ location }) => {
            if (process.env.NODE_ENV !== 'development') {
                window.location.reload();
            }

            return (
                <a href={getServiceURL(`..${location.pathname}`)}>
                    Redirect to Wicket
                </a>
            );
        }}
    />
);

export const apiDocRoute = (
    <Route
        path="/swagger"
        component={({ location }) => {
            if (process.env.NODE_ENV !== 'development') {
                window.location.reload();
            }

            return (
                <a href={getServiceURL(`/${location.pathname}`)}>
                    Redirect to ApiDoc
                </a>
            );
        }}
    />
);

export const publicRoute = (
    <Route
        path={`${prefix}public/:category/:type?/:id?`}
        render={(props) => <FormPage {...props} isPublic />}
    />
);

function AuthorizedRoutes(
    {
        alertMessage,
        locale,
    },
) {
    const getRoutesWithLocation = (location) => (
        <Switch location={location}>
            {wicketRoute}
            {apiDocRoute}
            {publicRoute}
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
                path={`${prefix}:category/:type/:id?`}
                component={FormPage}
            />
            <Route
                path={`${prefix}:category`}
                component={ListPage}
            />
        </Switch>
    );

    return (
        <>
            <Helmet>
                <html lang={locale} />
            </Helmet>
            <GlobalNavigation />
            {alertMessage ? (
                <Container fluid>
                    <Alert color="danger">
                        {alertMessage}
                    </Alert>
                </Container>
            ) : undefined}
            <ModalRoutes getRoutesWithLocation={getRoutesWithLocation} />
        </>
    );
}

AuthorizedRoutes.propTypes = {
    alertMessage: PropTypes.string,
    locale: PropTypes.string,
};

AuthorizedRoutes.defaultProps = {
    alertMessage: undefined,
    locale: 'en',
};

const mapStateToProps = ({ authentication }) => ({
    alertMessage: authentication.alertMessage,
    locale: authentication.user.locale,
});

export default connect(mapStateToProps)(AuthorizedRoutes);
