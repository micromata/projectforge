import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { setListFilter } from '../../../actions';
import ActionGroup from '../../../components/base/page/action/Group';
import LayoutGroup from '../../../components/base/page/layout/Group';
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
import { getNamedContainer } from '../../../utilities/layout';
import { buttonPropType } from '../../../utilities/propTypes';

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
        const {
            actions,
            filter,
            namedContainers,
            setFilter,
        } = this.props;
        // TODO: REPLACE DATE AND TIME WITH PICKERS
        return (
            <Card>
                <CardBody>
                    <form>
                        <Input
                            label="[Suchfilter]"
                            id="searchString"
                            value={filter.searchString}
                            onChange={this.handleInputChange}
                        />
                        <Row>
                            <Col sm={8}>
                                <FormGroup row>
                                    <Label sm={2}>[Optionen]</Label>
                                    <Col sm={10}>
                                        <LayoutGroup
                                            {...getNamedContainer('filterOptions', namedContainers)}
                                            data={filter}
                                            changeDataField={setFilter}
                                        />
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
                    </form>
                </CardBody>
            </Card>
        );
    }
}

SearchFilter.propTypes = {
    setFilter: PropTypes.func.isRequired,
    actions: PropTypes.arrayOf(buttonPropType),
    filter: PropTypes.shape({
        searchString: PropTypes.string,
        maxRows: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    }),
    namedContainers: PropTypes.arrayOf(PropTypes.shape({})),
};

SearchFilter.defaultProps = {
    actions: [],
    filter: {
        searchString: '',
        maxRows: 50,
    },
    namedContainers: [],
};

const mapStateToProps = state => ({
    filter: state.listPage.filter,
    actions: state.listPage.ui.actions,
    namedContainers: state.listPage.ui.namedContainers,
});

const actions = {
    setFilter: setListFilter,
};

export default connect(mapStateToProps, actions)(SearchFilter);
