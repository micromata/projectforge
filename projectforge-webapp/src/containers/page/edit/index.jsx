import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { setAllEditPageFields } from '../../../actions';
import ActionGroup from '../../../components/base/page/action/Group';
import TabNavigation from '../../../components/base/page/edit/TabNavigation';
import LayoutGroup from '../../../components/base/page/layout/Group';
import PageNavigation from '../../../components/base/page/Navigation';
import { Alert, Button, Container, TabContent, TabPane, } from '../../../components/design';
import LoadingContainer from '../../../components/design/loading-container';
import { getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import style from '../../ProjectForge.module.scss';

class EditPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            loading: true,
            activeTab: 'edit',
            error: undefined,
            layout: [],
            actions: [],
            title: 'Edit',
        };

        this.toggleTab = this.toggleTab.bind(this);
        this.fetchLayout = this.fetchLayout.bind(this);
    }

    componentDidMount() {
        this.fetchLayout();
    }

    fetchLayout() {
        this.setState({
            loading: true,
            error: undefined,
            layout: [],
            actions: [],
            title: '',
        });

        fetch(
            getServiceURL('books/edit', { id: 170 }),
            {
                method: 'GET',
                credentials: 'include',
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then((json) => {
                const { updateValues } = this.props;

                updateValues({
                    id: 170,
                    ...json.data,
                });

                this.setState({
                    loading: false,
                    ...json.ui,
                });
            })
            .catch(error => this.setState({
                error,
                loading: false,
            }));
    }

    toggleTab(event) {
        const { activeTab } = this.state;

        if (activeTab === event.target.id) {
            return;
        }

        this.setState({
            activeTab: event.target.id,
        });
    }

    render() {
        const {
            actions,
            activeTab,
            error,
            layout,
            loading,
            title,
        } = this.state;

        const { values } = this.props;

        if (error) {
            return (
                <Alert color="danger">
                    <h4>[Failed to Fetch Design]</h4>
                    <p>[Description Here]</p>
                    <Button onClick={this.fetchLayout}>[Retry]</Button>
                </Alert>
            );
        }

        return (
            <LoadingContainer loading={loading}>
                <PageNavigation current={title} />
                <TabNavigation
                    tabs={{
                        edit: title,
                        history: '[History]',
                    }}
                    toggleTab={this.toggleTab}
                    activeTab={activeTab}
                />
                <TabContent
                    activeTab={activeTab}
                    className={style.tabContent}
                >
                    <TabPane tabId="edit">
                        <Container fluid>
                            <LayoutGroup content={layout} values={values} />
                            <ActionGroup actions={actions} />
                        </Container>
                    </TabPane>
                </TabContent>
            </LoadingContainer>
        );
    }
}

EditPage.propTypes = {
    updateValues: PropTypes.func.isRequired,
    values: PropTypes.shape,
};

EditPage.defaultProps = {
    values: {},
};

const mapStateToProps = state => ({
    values: state.listPage.values,
});

const actions = {
    updateValues: setAllEditPageFields,
};

export default connect(mapStateToProps, actions)(EditPage);
