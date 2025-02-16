import PropTypes from 'prop-types';
import React from 'react';
import { connect, useSelector } from 'react-redux';
import { useLocation, useParams, useSearchParams } from 'react-router';
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
import { getServiceURL } from '../../../utilities/rest';
import style from '../../ProjectForge.module.scss';
import FormHistory from './history';

function FormPage(
    {
        isPublic = false,
        onCallAction,
        onCategorySwitch,
        onDataChange,
        onNewFormPage,
        onVariablesChange,
    },
) {
    const {
        type,
        category: currentCategory,
        id,
        tab,
    } = useParams();
    const category = useSelector(({ form }) => form.categories[currentCategory]) || {};
    const {
        data,
        isFetching,
        ui,
        validationErrors,
        variables,
    } = category;
    const location = useLocation();
    const [searchParams] = useSearchParams();
    const { userAccess } = ui || {};

    React.useEffect(
        () => {
            if (location.state && location.state.noReload) {
                onCategorySwitch(
                    currentCategory,
                    location.state.newVariables || {},
                    location.state.merge,
                );
                return;
            }

            onNewFormPage(
                currentCategory,
                id,
                getServiceURL(
                    `${isPublic ? '/rsPublic/' : ''}${currentCategory}/${type || 'dynamic'}`,
                    {
                        ...Object.fromEntries(searchParams.entries()),
                        id,
                    },
                ),
                location.state,
            );
        },
        [
            currentCategory,
            id,
            location.state && location.state.noReload,
            location.state && location.state.newVariables,
        ],
    );

    const globalValidation = React.useMemo(() => {
        if (validationErrors === undefined) {
            return null;
        }
        const globalErrors = validationErrors.filter((entry) => entry.fieldId === undefined);

        if (globalErrors.length === 0) {
            return null;
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

    if (ui === undefined || ui.title === undefined) {
        return <LoadingContainer loading />;
    }

    const formBaseUrl = `/react/${currentCategory}/edit/${id}`;
    const tabs = [
        {
            id: 'form',
            title: ui.title,
            link: formBaseUrl,
        },
    ];

    if (ui.showHistory === true) {
        tabs.push({
            id: 'history',
            title: getTranslation('label.historyOfChanges', ui.translations),
            link: `${formBaseUrl}/history`,
        });
    }

    return (
        <Container fluid>
            <TabNavigation
                tabs={tabs}
                activeTab={tab || 'form'}
            />
            <TabContent
                activeTab={tab || 'form'}
                className={style.tabContent}
            >
                <TabPane tabId="form">
                    <Container fluid>
                        <form>
                            <DynamicLayout
                                callAction={onCallAction}
                                data={data}
                                isFetching={isFetching}
                                options={{
                                    displayPageMenu: ui.pageMenu !== undefined,
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
                                    visible={tab === 'history'}
                                    userAccess={userAccess}
                                />
                            </Container>
                        </TabPane>
                    )}
            </TabContent>
        </Container>
    );
}

FormPage.propTypes = {
    onCallAction: PropTypes.func.isRequired,
    onCategorySwitch: PropTypes.func.isRequired,
    onDataChange: PropTypes.func.isRequired,
    onNewFormPage: PropTypes.func.isRequired,
    onVariablesChange: PropTypes.func.isRequired,
    isPublic: PropTypes.bool,
};

const mapStateToProps = () => ({});

const actions = {
    onCallAction: callAction,
    onCategorySwitch: switchFromCurrentCategory,
    onDataChange: setCurrentData,
    onNewFormPage: loadFormPage,
    onVariablesChange: setCurrentVariables,
};

export default connect(mapStateToProps, actions)(FormPage);
