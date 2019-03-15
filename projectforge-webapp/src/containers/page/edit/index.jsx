import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import ActionGroup from '../../../components/base/page/edit/ActionGroup';
import LayoutGroup from '../../../components/base/page/edit/layout/Group';
import TabNavigation from '../../../components/base/page/edit/TabNavigation';
import PageNavigation from '../../../components/base/page/Navigation';
import { Alert, Button, Container, TabContent, TabPane, } from '../../../components/design';
import LoadingContainer from '../../../components/design/loading-container';
import { getAuthenticationHeaders, handleHTTPErrors } from '../../../utilities/rest';
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

        const { userId, token } = this.props;

        fetch(
            '/rest_examples/edit/layout_example_response.json',
            {
                method: 'GET',
                headers: getAuthenticationHeaders(userId, token),
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(json => this.setState({
                loading: false,
                layout: json.layout,
                actions: json.actions,
                title: json.title,
            }))
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
                            <LayoutGroup content={layout} />
                            <ActionGroup actions={actions} />
                        </Container>
                    </TabPane>
                </TabContent>
            </LoadingContainer>
        );
    }
}

EditPage.propTypes = {
    userId: PropTypes.number.isRequired,
    token: PropTypes.string.isRequired,
};

const mapStateToProps = state => ({
    userId: state.authentication.user.id,
    token: state.authentication.user.token,
});

export default connect(mapStateToProps)(EditPage);
