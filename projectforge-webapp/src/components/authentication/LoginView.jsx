import React from 'react';
import PropTypes from 'prop-types';
import { faBell, faExclamationTriangle } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
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
import style from './Authentication.module.scss';

// TODO: INCLUDE TRANSLATION
function LoginView({ motd, administratorLoginNeeded, error }) {
    return (
        <div className={style.loginPanel}>
            <Card>
                <CardBody>
                    <CardTitle className={style.cardTitle}>[login.title]</CardTitle>
                    {administratorLoginNeeded
                        ? (
                            <Alert color="danger" className={style.alert}>
                                <FontAwesomeIcon icon={faBell} />
                                [login.adminLoginRequired]
                            </Alert>
                        )
                        : undefined
                    }
                    {error
                        ? (
                            <Alert color="danger" className={style.alert}>
                                <FontAwesomeIcon icon={faBell} />
                                {` ${error}`}
                            </Alert>
                        )
                        : undefined

                    }
                    {motd
                        ? (
                            <Alert color="primary" className={style.alert}>
                                <FontAwesomeIcon icon={faExclamationTriangle} />
                                {` ${motd}`}
                            </Alert>
                        )
                        : undefined
                    }
                    <Form>
                        <FormGroup row className={style.formGroup}>
                            <Label for="username" sm={3}><strong>[Username]</strong></Label>
                            <Col sm={9}>
                                <Input
                                    type="text"
                                    name="username"
                                    id="username"
                                    placeholder="[Username]"
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
                                        <Input type="checkbox" id="keepSignedIn" />
                                        <strong>[Keep Signed In]</strong>
                                    </Label>
                                </FormGroup>
                            </Col>
                        </FormGroup>
                        <Button color="success" block>[Login]</Button>
                    </Form>
                </CardBody>
            </Card>
        </div>
    );
}

LoginView.propTypes = {
    administratorLoginNeeded: PropTypes.bool,
    error: PropTypes.string,
    motd: PropTypes.string,
};

LoginView.defaultProps = {
    administratorLoginNeeded: false,
    error: undefined,
    motd: undefined,
};

export default LoginView;
