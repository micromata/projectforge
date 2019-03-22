import React from 'react';
import Card from 'reactstrap/es/Card';
import CardBody from 'reactstrap/es/CardBody';
import Col from 'reactstrap/es/Col';
import FormGroup from 'reactstrap/es/FormGroup';
import BootstrapInput from 'reactstrap/es/Input';
import BootstrapLabel from 'reactstrap/es/Label';
import Row from 'reactstrap/es/Row';
import Input from '../../components/design/input';
import CheckBox from '../../components/design/input/CheckBox';
import UncontrolledSelect from '../../components/design/input/UncontrolledSelect';

function InputTestPage() {
    return (
        <Card>
            <CardBody>
                <h1>Neue Input Felder</h1>
                <Row>
                    <Col>
                        <h2>Farben</h2>
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
                    <Col sm>
                        <h2>Zusätzlicher Titel</h2>
                        <Input
                            type="text"
                            label="Haupttitel"
                            id="subLine"
                            additionalLabel="Zusätzlicher Titel"
                        />
                        <Input
                            type="text"
                            label="Haupttitel mit Farbe"
                            id="subLineColor"
                            color="primary"
                            additionalLabel="Zusätzlicher Titel ist nicht betroffen"
                        />
                        <h2>CheckBox</h2>
                        <CheckBox
                            id="checkbox-primary"
                            label="Primary"
                            color="primary"
                        />
                        <CheckBox
                            id="checkbox-secondary"
                            label="Secondary"
                            color="secondary"
                        />
                        <CheckBox
                            id="checkbox-success"
                            label="Success"
                            color="success"
                        />
                        <CheckBox
                            id="checkbox-danger"
                            label="Danger"
                            color="danger"
                        />
                        <CheckBox
                            id="checkbox-warning"
                            label="Warning"
                            color="warning"
                        />
                        <CheckBox
                            id="checkbox-info"
                            label="Info"
                            color="info"
                        />
                        <CheckBox
                            id="checkbox-default"
                            label="Default"
                        />
                    </Col>
                    <Col sm>
                        <h2>Select</h2>
                        <UncontrolledSelect
                            id="select"
                            label="Typ"
                            options={[
                                {
                                    value: 'book',
                                    title: 'Buch',
                                },
                                {
                                    value: 'magazine',
                                    title: 'Magazin',
                                },
                                {
                                    value: 'article',
                                    title: 'Artikel',
                                },
                                {
                                    value: 'newspaper',
                                    title: 'Zeitung',
                                },
                                {
                                    value: 'periodical',
                                    title: 'Periodika',
                                },
                                {
                                    value: 'film',
                                    title: 'Film (Datenträger)',
                                },
                                {
                                    value: 'software',
                                    title: 'Software (Datenträger)',
                                },
                                {
                                    value: 'misc',
                                    title: 'Sonstiges',
                                },
                            ]}
                        />
                    </Col>
                    <Col sm style={{ fontSize: '14px' }}>
                        <h1>Alte Bootstrap Inputs</h1>
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
