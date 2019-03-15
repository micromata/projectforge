import { faBell, faExclamationTriangle } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import {
    Alert,
    Button,
    Card,
    CardBody,
    CardTitle,
    Col,
    Form,
    FormGroup,
    Input,
    Label,
} from '../design';
import LoadingContainer from '../design/loading-container';
import style from './Authentication.module.scss';

// TODO: INCLUDE TRANSLATION
class LoginView extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            username: '',
            password: '',
            keepSignedIn: false,
        };

        this.handleInputChange = this.handleInputChange.bind(this);
        this.handleFormSubmit = this.handleFormSubmit.bind(this);
    }

    handleInputChange(event) {
        const {
            id,
            value,
            type,
            checked,
        } = event.target;

        this.setState({
            [id]: type === 'checkbox' ? checked : value,
        });
    }

    handleFormSubmit(event) {
        event.preventDefault();

        const { login } = this.props;
        const { username, password, keepSignedIn } = this.state;
        login(username, password, keepSignedIn);
    }

    render() {
        const {
            motd,
            administratorLoginNeeded,
            error,
            loading,
        } = this.props;
        const { username, password, keepSignedIn } = this.state;

        return (
            <LoadingContainer className={style.loginPanel} loading={loading}>
                <Card>
                    <CardBody>
                        <CardTitle className={style.cardTitle}>[login.title]</CardTitle>
                        {administratorLoginNeeded
                            ? (
                                <Alert
                                    color="danger"
                                    className={style.alert}
                                    aria-label="administratorLoginNeededAlert"
                                >
                                    <FontAwesomeIcon icon={faBell} />
                                    [login.adminLoginRequired]
                                </Alert>
                            )
                            : undefined
                        }
                        {error
                            ? (
                                <Alert
                                    color="danger"
                                    className={style.alert}
                                    aria-label="errorAlert"
                                >
                                    <FontAwesomeIcon icon={faBell} />
                                    {` ${error}`}
                                </Alert>
                            )
                            : undefined

                        }
                        {motd
                            ? (
                                <Alert
                                    color="primary"
                                    className={style.alert}
                                    aria-label="motdAlert"
                                >
                                    <FontAwesomeIcon icon={faExclamationTriangle} />
                                    {` ${motd}`}
                                </Alert>
                            )
                            : undefined
                        }
                        <Form onSubmit={this.handleFormSubmit}>
                            <FormGroup row className={style.formGroup}>
                                <Label for="username" sm={3}><strong>[Username]</strong></Label>
                                <Col sm={9}>
                                    <Input
                                        type="text"
                                        name="username"
                                        id="username"
                                        placeholder="[Username]"
                                        onChange={this.handleInputChange}
                                        value={username}
                                        autoComplete="username"
                                        aria-label="username"
                                    />
                                </Col>
                            </FormGroup>
                            <FormGroup row className={style.formGroup}>
                                <Label for="password" sm={3}><strong>[Password]</strong></Label>
                                <Col sm={9}>
                                    <Input
                                        type="password"
                                        name="password"
                                        id="password"
                                        placeholder="[Password]"
                                        onChange={this.handleInputChange}
                                        value={password}
                                        autoComplete="current-password"
                                        aria-label="password"
                                    />
                                </Col>
                            </FormGroup>
                            <FormGroup row className={style.formGroup}>
                                <Col
                                    sm={{
                                        size: 9,
                                        offset: 3,
                                    }}
                                >
                                    <FormGroup check>
                                        <Label check>
                                            <Input
                                                type="checkbox"
                                                name="checkbox"
                                                id="keepSignedIn"
                                                onChange={() => {
                                                }}
                                                onClick={this.handleInputChange}
                                                aria-label="keepSignedIn"
                                                checked={keepSignedIn}
                                            />
                                            <strong>[Keep Signed In]</strong>
                                        </Label>
                                    </FormGroup>
                                </Col>
                            </FormGroup>
                            <Button color="success" block aria-label="login">[Login]</Button>
                        </Form>
                    </CardBody>
                </Card>
            </LoadingContainer>
        );
    }
}

LoginView.propTypes = {
    loading: PropTypes.bool.isRequired,
    login: PropTypes.func.isRequired,
    administratorLoginNeeded: PropTypes.bool,
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    motd: PropTypes.string,
};

LoginView.defaultProps = {
    administratorLoginNeeded: false,
    error: undefined,
    motd: undefined,
};

export default LoginView;
