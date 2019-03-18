import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { loadList } from '../../../actions';
import PageNavigation from '../../../components/base/page/Navigation';
import LoadingContainer from '../../../components/design/loading-container';

class ListPage extends Component {
    componentDidMount() {
        const { load } = this.props;

        load();
    }

    render() {
        const { loading } = this.props;

        /*
        TODO:
        - Move state to redux
        - Add Filters
        - Add List
        - (Add EditPage Actions)
         */

        return (
            <LoadingContainer loading={loading}>
                <PageNavigation current="Loading" />
            </LoadingContainer>
        );
    }
}

ListPage.propTypes = {
    loading: PropTypes.bool.isRequired,
    load: PropTypes.func.isRequired,
};

const mapStateToProps = state => ({
    loading: state.listPage.loading,
});

const actions = {
    load: loadList,
};

export default connect(mapStateToProps, actions)(ListPage);
