import React from 'react';
import Card from 'reactstrap/es/Card';
import CardBody from 'reactstrap/es/CardBody';
import Col from 'reactstrap/es/Col';
import FormGroup from 'reactstrap/es/FormGroup';
import BootstrapInput from 'reactstrap/es/Input';
import BootstrapLabel from 'reactstrap/es/Label';
import Row from 'reactstrap/es/Row';
import Input from '../../components/design/input';

function InputTestPage() {
    return (
        <Card>
            <CardBody>
                <Row>
                    <Col>
                        <h1>New Input Fields</h1>
                        <Input
                            type="text"
                            label="Primary"
                            id="primary"
                            color="primary"
                        />
                        <Input
                            type="text"
                            label="Secondary"
                            id="secondary"
                            color="secondary"
                        />
                        <Input
                            type="text"
                            label="Success"
                            id="success"
                            color="success"
                        />
                        <Input
                            type="text"
                            label="Danger"
                            id="danger"
                            color="danger"
                        />
                        <Input
                            type="text"
                            label="Warning"
                            id="warning"
                            color="warning"
                        />
                        <Input
                            type="text"
                            label="Info"
                            id="info"
                            color="info"
                        />
                        <Input
                            type="text"
                            label="Default"
                            id="default"
                        />
                    </Col>
                    <Col sm style={{ fontSize: '14px' }}>
                        <h1>Old Bootstrap Inputs</h1>
                        <FormGroup row>
                            <Col sm={2}>
                                <BootstrapLabel>
                                    Mail
                                </BootstrapLabel>
                            </Col>
                            <Col sm={10}>
                                <BootstrapInput />
                            </Col>
                        </FormGroup>
                        <FormGroup row>
                            <Col sm={2}>
                                <BootstrapLabel>
                                    Passwort
                                </BootstrapLabel>
                            </Col>
                            <Col sm={10}>
                                <BootstrapInput />
                            </Col>
                        </FormGroup>
                        <FormGroup row>
                            <Col sm={2}>
                                <BootstrapLabel>
                                    Name
                                </BootstrapLabel>
                            </Col>
                            <Col sm={10}>
                                <BootstrapInput />
                            </Col>
                        </FormGroup>
                        <FormGroup row>
                            <Col sm={2}>
                                <BootstrapLabel>
                                    Telefon
                                </BootstrapLabel>
                            </Col>
                            <Col sm={10}>
                                <BootstrapInput />
                            </Col>
                        </FormGroup>
                    </Col>
                </Row>
            </CardBody>
        </Card>
    );
}

export default InputTestPage;
