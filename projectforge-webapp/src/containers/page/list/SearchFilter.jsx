import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { setListFilter } from '../../../actions';
import ActionGroup from '../../../components/base/page/action/Group';
import {
    Card,
    CardBody,
    Col,
    FormGroup,
    Input,
    Label,
    Row,
    Select,
} from '../../../components/design';
import { buttonPropType } from '../../../utilities/propTypes';
import FilterButtons from './FilterButtons';

class SearchFilter extends Component {
    constructor(props) {
        super(props);

        this.handleInputChange = this.handleInputChange.bind(this);
        this.handleSelectChange = this.handleSelectChange.bind(this);
    }

    handleInputChange(event) {
        const { setFilter } = this.props;

        setFilter(event.target.id, event.target.value);
    }

    handleSelectChange(value) {
        const { setFilter } = this.props;

        setFilter('maxRows', value);
    }

    render() {
        const { filter, actions } = this.props;
        // TODO: REPLACE DATE AND TIME WITH PICKERS
        return (
            <Card>
                <CardBody>
                    <Input
                        label="[Suchfilter]"
                        id="searchString"
                        value={filter.searchString}
                        onChange={this.handleInputChange}
                    />
                    <Row>
                        <Col sm={8}>
                            <Input
                                label="[Änderungszeitraum]"
                                id="changePeriod"
                            />
                        </Col>
                        <Col sm={4}>
                            <Input
                                label="[geändert durch]"
                                id="user"
                            />
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
                            <Select
                                selected={filter.maxRows}
                                setSelected={this.handleSelectChange}
                                id="maxRows"
                                label="[Seitengröße]"
                                options={['25', '50', '100', '200', '500', '1000']}
                            />
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
}

SearchFilter.propTypes = {
    setFilter: PropTypes.func.isRequired,
    filter: PropTypes.shape({
        searchString: PropTypes.string,
        maxRows: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    }),
    actions: PropTypes.arrayOf(buttonPropType),
};

SearchFilter.defaultProps = {
    actions: [],
    filter: {
        searchString: '',
        maxRows: 50,
    },
};

const mapStateToProps = state => ({
    filter: state.listPage.filter,
    actions: state.listPage.ui.actions,
});

const actions = {
    setFilter: setListFilter,
};

export default connect(mapStateToProps, actions)(SearchFilter);
