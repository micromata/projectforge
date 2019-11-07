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

        const todoDone = { color: 'green' };

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
                        <h1>ToDo&apos;s (Fin)</h1>
                        <ol>
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
                            <li>
                                Prepare nested, indexed properties, such as
                                {' '}
                                <code>invoice.positions[2].amount</code>
                                <br />
                                See /incomingInvoice
                            </li>
                        </ol>
                        <h1>Done</h1>
                        <ol style={todoDone}>
                            <li>
                                Clear Autocomplete (EditableMultiValueLabel) crash.
                            </li>
                            <li>
                                ReactSelect.jsx tooltip random id.
                            </li>
                            <li>Finishing time sheet editing (recents)</li>
                            <li>
                                Open Popover with focus in Inputfield after selecting a new
                                field in list&apos;s search filter.
                            </li>
                            <li>Update React-Select (fixes warning)</li>
                            <li>
                                TaskSelect panel: collapse search fields to have more a feeling
                                of a bread crumb...
                            </li>
                            <li>Translations in SearchFilter.jsx</li>
                            <li>MagicFilter: send filter settings to server on search action</li>
                            <li>
                                Bugs after dynamic layout refactoring
                                <ol>
                                    <li>
                                        Address list page
                                        <br />
                                        <code>
                                            Failed prop type: Invalid prop `translations` of type
                                            `array` supplied to `FavoritesPanel`, expected `object`.
                                            in FavoritesPanel (at SearchFilter.jsx:125)
                                        </code>
                                    </li>
                                </ol>
                            </li>
                            <li>
                                Redirect menu entries on edit page (address - print view)
                            </li>
                            <li>
                                Display Logo
                                <ol>
                                    <li>
                                        Configuration: projectforge.properties:
                                        projectforge.logoFile=Micromata.png
                                    </li>
                                    <li>Logo url in rsPublic/systemStatus (e. g. logo.png)</li>
                                    <li>
                                        Logo service: rsPublic/logo.png (rsPublic/&lt;logoUrl&gt;)
                                    </li>
                                </ol>
                            </li>
                            <li>Display global validation errors of forms</li>
                            <li>
                                Clone button (e. g. address): reload page with data sent by server
                                as response after fetching clone
                            </li>
                            <li>
                                Display field validation errors in ReactSelect and date input
                                fields
                            </li>
                            <li>
                                Edit page in Modals: handle tabs (especially history), see time
                                sheet edit page as modal of CalendarPage.
                            </li>
                            <li>Redirect after logout</li>
                            <li>Enable localized customized pages (login page, setup page etc.)</li>
                            <li>
                                Redirect to setup-page if ProjectForge isn&apos;t initialized on
                                first usage.
                            </li>
                            <li>
                                Edit / rename entry of favorite filter, e. g. calendar page (star
                                icon)
                            </li>
                            <li>Highlight last edited entry in list page</li>
                            <li>
                                In der Listenansicht werden nur die geraden (hellen) Zeilen
                                gehighlighted nach der Editierseite (even-odd).
                            </li>
                            <li>
                                Bug: States of Inputfields!
                            </li>
                            <li>Change time of day in calendar events.</li>
                            <li>
                                TimesheetEditTaskAndKost2.jsx: set kost2list on initial call and
                                update of kost2list after task selections. See classic version.
                                <i>Part of TaskTree refactor</i>
                            </li>
                            <li>
                                Design improvements for selection of task tree (e. g. time
                                sheet editing). Highlighting of current active breadcrumb,
                                smaller search bar, no table head row. Collapse like Apple Finder.
                            </li>
                            <li>
                                Set date of calendar view after editing timesheet or event.
                            </li>
                            <li>Calendar refetch events after deletion/creation etc.</li>
                            <li>System alert message (front end side)</li>
                            <li>
                                Customized Data Image in List Page not working
                            </li>
                            <li>
                                Autocompletion for input fields (see outgoing-mail), worked in older
                                versions (before dynamic rendering).
                            </li>
                        </ol>
                    </Col>
                    <Col>
                        <h1>ToDo&apos;s (Kai)</h1>
                        <ol>
                            <li>Adressbücher fehlen zwei row title.</li>
                            <li>List pages: addresses, tasks etc.</li>
                            <li>
                                Search filter
                                <ol>
                                    <li>
                                        <code>
                                            Querying fields of type &#39;class
                                            org.projectforge.business.address.AddressStatus&#39; not
                                            yet implemented.
                                        </code>
                                    </li>
                                </ol>
                            </li>
                            <li>Setup page</li>
                            <li>Message of the day</li>
                            <li>CSRF</li>
                        </ol>
                        <h1>Done</h1>
                        <ol style={todoDone}>
                            <li>System alert message (server side)</li>
                            <li>Drag&Drop/resize of calendar events</li>
                            <li>Calendar events (especially recurrences)</li>
                            <li>updateFilter-Rest call for favorites</li>
                            <li>
                                Remove Apple-Control chars in all input fields (generated by copy
                                and paste e. g. from Apple&apos; address book)
                            </li>
                            <li>Remove BookDO.task from data base.</li>
                            <li>
                                Edit of addresses: preserve address books without access of current
                                logged in user.
                            </li>
                            <li>
                                DynamicReactSelect: support of autocompletion and favorites (see
                                field communication language of edit page for addresses).
                            </li>
                            <li>
                                Calendar edit pages redirect url to /calendar
                            </li>
                            <li>Favorites</li>
                            <li>
                                switch from edit page time-sheets to calendar event and vica
                                versa. see classic version.
                                Via Action Buttons and UPDATE action
                            </li>
                            <li>
                                <code>
                                    java.lang.UnsupportedOperationException: null
                                    at java.sql.Date.toInstant(Date.java:304)
                                </code>
                            </li>
                            <li>
                                Fix calendar issues, if the user&apos;s time zone differs from the
                                browser&apos;s time zone. BigCalendar doesn&apos;t support this. The
                                client should send his time zone for getting the events for the
                                matching calendar week, otherwise the calendar get the events
                                for the week or day before and can&apos;t display them.
                            </li>
                            <li>Loading plugins failed after mgc-update in executable jar.</li>
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
                        <h1>Done</h1>
                        <ol style={todoDone}>
                            <li>Clone button of timesheets and calendar events</li>
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
