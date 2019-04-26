import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { changeEditFormField, loadEditPage } from '../../../actions';
import Navigation from '../../../components/base/navigation';
import ActionGroup from '../../../components/base/page/action/Group';
import TabNavigation from '../../../components/base/page/edit/TabNavigation';
import LayoutGroup from '../../../components/base/page/layout/LayoutGroup';
import PageNavigation from '../../../components/base/page/Navigation';
import { Alert, Container, TabContent, TabPane, } from '../../../components/design';
import LoadingContainer from '../../../components/design/loading-container';
import { getTranslation } from '../../../utilities/layout';
import { getObjectFromQuery } from '../../../utilities/rest';
import style from '../../ProjectForge.module.scss';
import EditHistory from './history';

class EditPage extends React.Component {
    componentDidMount() {
        const { load, location, match } = this.props;

        load(match.params.category, match.params.id, getObjectFromQuery(location.search || ''));
    }

    render() {
        const {
            changeDataField,
            data,
            variables,
            error,
            loading,
            ui,
            validation,
            match,
        } = this.props;

        const { category, id } = match.params;

        if (error) {
            return (
                <Alert color="danger">
                    <h4>[Failed to Fetch Design]</h4>
                    <p>[Description Here]</p>
                </Alert>
            );
        }

        const activeTab = match.params.tab || 'edit';

        const tabs = [];

        if (id) {
            const baseUrl = `/${category}/edit/${id}`;

            tabs.push({
                id: 'edit',
                title: ui.title,
                link: baseUrl,
            }, {
                id: 'history',
                title: getTranslation('label.historyOfChanges', ui.translations),
                link: `${baseUrl}/history`,
            });
        }

        return (
            <LoadingContainer loading={loading}>
                <PageNavigation current={ui.title}>
                    <Navigation entries={ui.pageMenu || []} />
                </PageNavigation>
                <TabNavigation
                    tabs={tabs}
                    activeTab={activeTab}
                />
                <TabContent
                    activeTab={activeTab}
                    className={style.tabContent}
                >
                    <TabPane tabId="edit">
                        <Container fluid>
                            <form>
                                <LayoutGroup
                                    content={ui.layout}
                                    data={data}
                                    variables={variables}
                                    translations={ui.translations}
                                    changeDataField={changeDataField}
                                    validation={validation}
                                />
                                <ActionGroup actions={ui.actions} />
                            </form>
                        </Container>
                    </TabPane>
                    {id
                        ? (
                            <TabPane tabId="history">
                                <Container fluid>
                                    <EditHistory
                                        category={category}
                                        id={id}
                                        translations={ui.translations}
                                    />
                                </Container>
                            </TabPane>
                        )
                        : undefined}
                </TabContent>
            </LoadingContainer>
        );
    }
}

EditPage.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    match: PropTypes.shape({}).isRequired,
    load: PropTypes.func.isRequired,
    location: PropTypes.shape({
        hash: PropTypes.string,
        pathname: PropTypes.string,
        search: PropTypes.string,
    }).isRequired,
    ui: PropTypes.shape({
        translations: PropTypes.shape({}),
    }).isRequired,
    validation: PropTypes.shape({}),
    error: PropTypes.any,
    data: PropTypes.shape({}),
    variables: PropTypes.shape({}),
    loading: PropTypes.bool,
    afterEdit: PropTypes.func,
};

EditPage.defaultProps = {
    data: [],
    variables: {},
    error: undefined,
    loading: false,
    validation: {},
    afterEdit: undefined,
};

const mapStateToProps = state => ({
    ui: state.editPage.ui,
    error: state.editPage.error,
    loading: state.editPage.loading,
    data: state.editPage.data,
    variables: state.editPage.variables,
    validation: state.editPage.validation,
});

const actions = {
    load: loadEditPage,
    changeDataField: changeEditFormField,
};

export default connect(mapStateToProps, actions)(EditPage);
