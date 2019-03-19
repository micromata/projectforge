import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { setListFilter } from '../../../actions';
import ActionGroup from '../../../components/base/page/action/Group';

class FilterButtons extends Component {
    constructor(props) {
        super(props);

        this.handleFilterClick = this.handleFilterClick.bind(this);
        this.buildFilterAction = this.buildFilterAction.bind(this);
    }

    buildFilterAction(id, title) {
        const { [id]: checked } = this.props;

        let style;

        if (id === 'deleted' && checked) {
            style = 'danger';
        }

        return {
            checked,
            title,
            style,
            filterId: id,
            handleClick: this.handleFilterClick,
            type: 'checkbox',
        };
    }

    handleFilterClick(action) {
        const { setFilter } = this.props;

        setFilter(action.filterId, !action.checked);
    }

    render() {
        return (
            <ActionGroup
                actions={[
                    this.buildFilterAction('present', '[vorhanden]'),
                    this.buildFilterAction('missed', '[vermisst]'),
                    this.buildFilterAction('disposed', '[entsorgt]'),
                    this.buildFilterAction('deleted', '[nur gelöschte]'),
                    this.buildFilterAction('searchHistory', '[Historie]'),
                ]}
            />
        );
    }
}

FilterButtons.propTypes = {
    setFilter: PropTypes.func.isRequired,
    present: PropTypes.bool,
    missed: PropTypes.bool,
    disposed: PropTypes.bool,
    deleted: PropTypes.bool,
    searchHistory: PropTypes.bool,
};

FilterButtons.defaultProps = {
    present: true,
    missed: false,
    disposed: false,
    deleted: false,
    searchHistory: false,
};

const mapStateToProps = state => ({
    ...state.listPage.filter,
});

const actions = {
    setFilter: setListFilter,
};

export default connect(mapStateToProps, actions)(FilterButtons);
