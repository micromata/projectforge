import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import CreatableSelect from 'react-select/lib/Creatable';
import { setListFilter } from '../../../actions';
import ActionGroup from '../../../components/base/page/action/Group';
import EditableMultiValueLabel from '../../../components/base/page/layout/EditableMultiValueLabel';
import LayoutGroup from '../../../components/base/page/layout/LayoutGroup';
import { Card, CardBody, Col, FormGroup, Label, Row, Select, } from '../../../components/design';
import { getNamedContainer } from '../../../utilities/layout';
import { buttonPropType } from '../../../utilities/propTypes';

class SearchFilter extends Component {
    constructor(props) {
        super(props);

        this.state = {
            filter: {},
        };

        this.handleInputChange = this.handleInputChange.bind(this);
        this.handleSelectChange = this.handleSelectChange.bind(this);
        this.handleFilterChange = this.handleFilterChange.bind(this);
        this.getOptionLabel = this.getOptionLabel.bind(this);
    }

    getOptionLabel(option) {
        // disable eslint because variable is provided by react-select and can't be changed.
        /* eslint-disable-next-line no-underscore-dangle */
        if (option.__isNew__) {
            return option.label;
        }

        const { filter } = this.state;
        const value = filter[option.id];

        return `${option.id}${value ? `: ${value}` : ''}`;
    }

    handleInputChange(event) {
        const { setFilter } = this.props;

        setFilter(event.target.id, event.target.value);
    }

    handleSelectChange(value) {
        const { setFilter } = this.props;

        setFilter('maxRows', value);
    }

    handleFilterChange(id, newValue) {
        const { setFilter } = this.props;

        setFilter(id, newValue);
        this.setState(({ filter }) => ({
            filter: {
                ...filter,
                [id]: newValue,
            },
        }));
    }

    render() {
        const {
            actions,
            filter,
            namedContainers,
            setFilter,
        } = this.props;
        const {
            filter: newFilter,
        } = this.state;

        console.log(filter);

        let options = [];
        const searchFilter = getNamedContainer('searchFilter', namedContainers);

        if (searchFilter) {
            options = searchFilter.content.map(option => ({
                ...option,
                value: option.key,
                label: option.id,
            }));
        }

        return (
            <Card>
                <CardBody>
                    <form>
                        <CreatableSelect
                            name="searchFilter"
                            options={options}
                            isClearable
                            isMulti
                            components={{
                                MultiValueLabel: EditableMultiValueLabel,
                            }}
                            setMultiValue={this.handleFilterChange}
                            values={newFilter}
                            getOptionLabel={this.getOptionLabel}
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
