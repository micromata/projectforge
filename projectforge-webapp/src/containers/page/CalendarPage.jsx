import React from 'react';
import { Button, Card, CardBody, Col, Row } from 'reactstrap';
import Select from 'react-select';
import makeAnimated from 'react-select/lib/animated';
import CalendarPanel from '../panel/CalendarPanel';
import { getServiceURL } from '../../utilities/rest';
import LoadingContainer from '../../components/design/loading-container';

class CalendarPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            date: new Date(),
            view: 'week',
            translations: undefined,
        }

        this.fetchInitial = this.fetchInitial.bind(this);
    }

    componentDidMount() {
        this.fetchInitial();
    }

    fetchInitial() {
        this.setState({ loading: true });
        fetch(getServiceURL('calendar/initial'), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(response => response.json())
            .then((json) => {
                const {
                    date,
                    view,
                    translations,
                } = json;
                this.setState({
                    loading: false,
                    date: new Date(date),
                    view,
                    translations,
                });
            })
            .catch(error => alert(`Internal error: ${error}`));
    }

    render() {
        const {
            loading,
            date,
            view,
            translations,
        } = this.state;
        if (!translations) {
            return <div>...</div>;
        }
        return (
            <LoadingContainer loading={loading}>
                <Card>
                    <CardBody>
                        <form>
                            <Row>
                                <Col sm={6}>
                                    <Select
                                        components={makeAnimated()}
                                        value=""
                                        isMulti
                                        options={[]}
                                        isClearable
                                        // getOptionValue={option => (option[valueProperty])}
                                        // getOptionLabel={getOptionLabel || (option => (option[labelProperty]))}
                                        // onChange={onChange}
                                        // loadOptions={loadOptions}
                                        // defaultOptions={defaultOptions}
                                        placeholder={translations['select.placeholder']}
                                    />
                                </Col>
                                <Col sm={6}>
                                    <Row>
                                        <Button
                                            color="primary"
                                            onClick={this.onSubmit}
                                            type="submit"
                                        >
                                            {translations.search}
                                        </Button>
                                    </Row>
                                </Col>
                            </Row>

                        </form>
                    </CardBody>
                </Card>
                <CalendarPanel
                    defautlDate={date}
                    defaultView={view}
                    topHeight="225px"
                />
            </LoadingContainer>
        );
    }
}

CalendarPage.propTypes = {};

CalendarPage.defaultProps = {};

export default CalendarPage;
