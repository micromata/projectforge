import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Card, CardBody, CardHeader, CardText, CardTitle, Col, Container, Row } from 'reactstrap';
import { loadUserStatus } from '../../actions';
import { getServiceURL } from '../../utilities/rest';

class IndexPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            translations: undefined,
        };

        this.fetchInitial = this.fetchInitial.bind(this);
    }

    componentDidMount() {
        this.fetchInitial();
    }

    componentDidUpdate({ location: nextLocation }) {
        const { location, loadUserStatus: checkAuthentication } = this.props;

        if (location.key === nextLocation.key) {
            return;
        }

        checkAuthentication();
    }

    fetchInitial() {
        const { loadUserStatus: checkAuthentication } = this.props;

        fetch(getServiceURL('index'), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then((response) => {
                if (response.status === 401) {
                    throw response.status;
                }

                return response.json();
            })
            .then((json) => {
                const {
                    translations,
                } = json;
                this.setState({
                    translations,
                });
            })
            .catch(() => checkAuthentication());
    }

    render() {
        const { translations } = this.state;

        if (!translations) {
            return (<div>{' '}</div>);
        }
        return (
            <Container>
                <Row>
                    <Col>
                        <a href="/wa/">
                            <Card>
                                <CardHeader>
                                    <CardTitle>
                                        {translations['goreact.index.classics.header']}
                                    </CardTitle>
                                </CardHeader>
                                <CardBody>
                                    <CardText>
                                        {translations['goreact.index.classics.body1']}
                                    </CardText>
                                    <CardText>
                                        {translations['goreact.index.classics.body2']}
                                    </CardText>
                                </CardBody>
                            </Card>
                        </a>
                    </Col>
                    <Col>
                        <Card>
                            <CardHeader>
                                <CardTitle>
                                    {translations['goreact.index.react.header']}
                                </CardTitle>
                            </CardHeader>
                            <CardBody>
                                <CardText>
                                    {translations['goreact.index.react.body1']}
                                </CardText>
                                <CardText>
                                    {translations['goreact.index.react.body2']}
                                </CardText>
                            </CardBody>
                        </Card>
                    </Col>
                    <Col>
                        <Card>
                            <CardHeader>
                                <CardTitle>
                                    {translations['goreact.index.both.header']}
                                </CardTitle>
                            </CardHeader>
                            <CardBody>
                                <CardText>
                                    {translations['goreact.index.both.body1']}
                                </CardText>
                                <CardText>
                                    {translations['goreact.index.both.body2']}
                                </CardText>
                            </CardBody>
                        </Card>
                    </Col>
                </Row>
                <Row>
                    <Col>
                        <p
                            style={{
                                marginTop: '10ex',
                                marginBottom: '5ex',
                                color: 'red',
                                fontWeight: 'bold',
                                fontSize: '18px',
                            }}
                        >
                            To-do&apos;s (most have to be done before going public)
                        </p>
                    </Col>
                </Row>
                <Row>
                    <Col>
                        <h1>ToDo&apos;s (F.)</h1>
                        <ol>
                            <li>
                                Can&apos;t delete last calendar.
                            </li>
                            <li>
                                Modification of text searches (in all fields, non field-specific)
                                is not possible.
                            </li>
                            <li>
                                Return key closes the popover, another Return click submits the
                                filter and requests a new list?
                            </li>
                            <li>
                                main.chunk.js with hash sum / version id, use service worker for
                                caching app
                            </li>
                            <li>
                                Backlog: Timepicker (substituion of current used),
                                and date-range picker.
                            </li>
                        </ol>
                    </Col>
                    <Col>
                        <h1>ToDo&apos;s (both)</h1>
                        <ol>
                            <li>Magic filter in list pages</li>
                            <li>
                                TaskTree
                                <mark>Keine Tasks gefunden</mark>
                                translation. task/tree
                            </li>
                            <li>
                                Markdown (AsciiDoc) View component for displaying dynamic content.
                            </li>
                            <li>List pagination and sorting</li>
                            <li>Selection component for time of day (timesheets, cal events)</li>
                            <li>
                                Registration of customized containers (e. g. for external plugins)
                            </li>
                        </ol>
                    </Col>
                </Row>
            </Container>
        );
    }
}

IndexPage.propTypes = {
    loadUserStatus: PropTypes.func.isRequired,
    location: PropTypes.shape({
        key: PropTypes.string,
    }).isRequired,
};

IndexPage.defaultProps = {};

const actions = {
    loadUserStatus,
};

export default connect(undefined, actions)(IndexPage);
