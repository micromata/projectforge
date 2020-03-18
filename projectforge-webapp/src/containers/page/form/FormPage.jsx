import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Route } from 'react-router-dom';
import {
    callAction,
    loadFormPage,
    setCurrentData,
    setCurrentVariables,
    switchFromCurrentCategory,
} from '../../../actions';
import DynamicLayout from '../../../components/base/dynamicLayout';
import TabNavigation from '../../../components/base/page/edit/TabNavigation';
import { Alert, Container, TabContent, TabPane } from '../../../components/design';
import LoadingContainer from '../../../components/design/loading-container';
import { getTranslation } from '../../../utilities/layout';
import { getObjectFromQuery, getServiceURL } from '../../../utilities/rest';
import style from '../../ProjectForge.module.scss';
import FormHistory from './history';

function FormPage(
    {
        category,
        isPublic,
        location,
        match,
        onCallAction,
        onCategorySwitch,
        onDataChange,
        onNewFormPage,
        onVariablesChange,
    },
) {
    const {
        data,
        ui,
        validationErrors,
        variables,
    } = category;
    let { search } = location;
    let { type } = match.params;
    const {
        category: currentCategory,
        id,
    } = match.params;

    // React router sometimes doesn't recognise the search.
    if (type && type.includes('?')) {
        // Map the first part of the split to page and the second part to search.
        [type, search] = type.split('?');
        // Prepend the question mark.
        search = `?${search}`;
    }


    React.useEffect(
        () => {
            if (location.state && location.state.noReload) {
                onCategorySwitch(currentCategory, location.state.newVariables || {});
                return;
            }

            onNewFormPage(
                currentCategory,
                id,
                getServiceURL(
                    `${isPublic ? '../rsPublic/' : ''}${currentCategory}/${type || 'dynamic'}`,
                    {
                        ...getObjectFromQuery(search || ''),
                        id,
                    },
                ),
            );
        },
        [currentCategory, id, location.state],
    );

    const globalValidation = React.useMemo(() => {
        if (validationErrors === undefined) {
            return <React.Fragment />;
        }
        const globalErrors = validationErrors.filter(entry => entry.fieldId === undefined);

        if (globalErrors.length === 0) {
            return <React.Fragment />;
        }

        return (
            <Alert color="danger">
                <ul>
                    {globalErrors.map(({ message, messageId }) => (
                        <li key={`form-page-global-validation-${messageId}`}>
                            {message}
                        </li>
                    ))}
                </ul>
            </Alert>
        );
    }, [validationErrors]);

    if (ui === undefined) {
        return <LoadingContainer loading />;
    }

    const tabs = [
        {
            id: 'form',
            title: ui.title,
            link: match.url,
        },
    ];

    if (ui.showHistory === true) {
        tabs.push({
            id: 'history',
            title: getTranslation('label.historyOfChanges', ui.translations),
            link: `${match.url}/history`,
        });
    }

    return (
        <Container fluid>
            <Route
                path={`${match.url}/:tab?`}
                render={({ match: tabMatch }) => (
                    <React.Fragment>
                        <TabNavigation
                            tabs={tabs}
                            activeTab={tabMatch.params.tab || 'form'}
                        />
                        <TabContent
                            activeTab={tabMatch.params.tab || 'form'}
                            className={style.tabContent}
                        >
                            <TabPane tabId="form">
                                <Container fluid>
                                    <form>
                                        <DynamicLayout
                                            callAction={onCallAction}
                                            data={data}
                                            options={{
                                                displayPageMenu: id !== undefined,
                                                setBrowserTitle: true,
                                                showActionButtons: true,
                                                showPageMenuTitle: false,
                                            }}
                                            setData={onDataChange}
                                            setVariables={onVariablesChange}
                                            ui={ui}
                                            validationErrors={validationErrors}
                                            variables={variables}
                                        >
                                            {globalValidation}
                                        </DynamicLayout>
                                    </form>
                                </Container>
                            </TabPane>
                            {ui.showHistory === true && id
                            && (
                                <TabPane tabId="history">
                                    <Container fluid>
                                        <FormHistory
                                            category={currentCategory}
                                            id={id}
                                            translations={ui.translations}
                                            visible={tabMatch.params.tab === 'history'}
                                        />
                                    </Container>
                                </TabPane>
                            )}
                        </TabContent>
                    </React.Fragment>
                )}
            />
        </Container>
    );
}

FormPage.propTypes = {
    location: PropTypes.shape({
        search: PropTypes.string,
        state: PropTypes.shape({
            noReload: PropTypes.bool,
        }),
    }).isRequired,
    match: PropTypes.shape({
        params: PropTypes.shape({
            category: PropTypes.string.isRequired,
            id: PropTypes.string,
            tab: PropTypes.string,
            type: PropTypes.string,
        }).isRequired,
    }).isRequired,
    onCallAction: PropTypes.func.isRequired,
    onCategorySwitch: PropTypes.func.isRequired,
    onDataChange: PropTypes.func.isRequired,
    onNewFormPage: PropTypes.func.isRequired,
    onVariablesChange: PropTypes.func.isRequired,
    category: PropTypes.shape({}),
};

FormPage.defaultProps = {
    category: {},
    isPublic: false,
};

const mapStateToProps = ({ form }, { match }) => ({
    category: form.categories[match.params.category],
});

const actions = {
    onCallAction: callAction,
    onCategorySwitch: switchFromCurrentCategory,
    onDataChange: setCurrentData,
    onNewFormPage: loadFormPage,
    onVariablesChange: setCurrentVariables,
};

export default connect(mapStateToProps, actions)(FormPage);
