import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import ActionGroup from '../../../components/base/page/action/Group';
import { Card, CardBody, Col, FormGroup, Input, Label, Row, } from '../../../components/design';
import { buttonPropType } from '../../../utilities/propTypes';
import FilterButtons from './FilterButtons';

function SearchFilter({ filter, actions }) {
    // TODO: REPLACE DATE AND TIME WITH PICKERS
    return (
        <Card>
            <CardBody>
                <FormGroup row>
                    <Label sm={2}>[Suchfilter]</Label>
                    <Col sm={10}>
                        <Input type="text" />
                    </Col>
                </FormGroup>
                <Row>
                    <Col sm={8}>
                        <FormGroup row>
                            <Label sm={2}>[Änderungszeitraum]</Label>
                            <Col sm={5}>
                                <Input type="date" />
                            </Col>
                            <Col sm={5}>
                                <Input type="time" />
                            </Col>
                        </FormGroup>
                    </Col>
                    <Col sm={4}>
                        <FormGroup row>
                            <Label sm={3}>[geändert durch]</Label>
                            <Col sm={4}>
                                <Input type="text" />
                            </Col>
                        </FormGroup>
                    </Col>
                </Row>
                <Row>
                    <Col sm={8}>
                        <FormGroup row>
                            <Label sm={2}>[Optionen]</Label>
                            <Col sm={10}>
                                <FilterButtons />
                            </Col>
                        </FormGroup>
                    </Col>
                    <Col sm={4}>
                        <FormGroup row>
                            <Label sm={4}>[Seitengröße]</Label>
                            <Col sm={4}>
                                <Input type="select">
                                    <option>25</option>
                                    <option>50</option>
                                    <option>100</option>
                                    <option>200</option>
                                    <option>500</option>
                                    <option>1000</option>
                                </Input>
                            </Col>
                        </FormGroup>
                    </Col>
                </Row>
                <FormGroup row>
                    <Col>
                        <ActionGroup
                            actions={actions}
                        />
                    </Col>
                </FormGroup>
            </CardBody>
        </Card>
    );
}

SearchFilter.propTypes = {
    filter: PropTypes.shape({}).isRequired,
    actions: PropTypes.arrayOf(buttonPropType),
};

SearchFilter.defaultProps = {
    actions: [],
};

const mapStateToProps = state => ({
    filter: state.listPage.filter,
    actions: state.listPage.ui.actions,
});

export default connect(mapStateToProps)(SearchFilter);
