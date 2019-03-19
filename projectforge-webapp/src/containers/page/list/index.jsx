import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { loadList } from '../../../actions';
import PageNavigation from '../../../components/base/page/Navigation';
import LoadingContainer from '../../../components/design/loading-container';
import { buttonPropType } from '../../../utilities/propTypes';
import SearchFilter from './SearchFilter';

class ListPage extends Component {
    componentDidMount() {
        const { load } = this.props;

        load();
    }

    render() {
        const { loading, title } = this.props;

        /*
        TODO:
        - Move state to redux
        - Add Filters
        - Add List
        - (Add EditPage Actions)
         */

        return (
            <LoadingContainer loading={loading}>
                <PageNavigation current={title} />
                <SearchFilter />
            </LoadingContainer>
        );
    }
}

ListPage.propTypes = {
    loading: PropTypes.bool.isRequired,
    load: PropTypes.func.isRequired,
    layout: PropTypes.shape({
        actions: PropTypes.arrayOf(PropTypes.shape(buttonPropType)),
        layout: PropTypes.arrayOf(PropTypes.shape({

        })),
        title: PropTypes.string,
    }).isRequired,
};

const mapStateToProps = state => ({
    loading: state.listPage.loading,
    layout: state.listPage.layout,
});

const actions = {
    load: loadList,
};

export default connect(mapStateToProps, actions)(ListPage);
