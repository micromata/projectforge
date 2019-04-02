import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';
import { loadList } from '../../../actions';
import LayoutGroup from '../../../components/base/page/layout/Group';
import PageNavigation from '../../../components/base/page/Navigation';
import { Button, NavItem } from '../../../components/design';
import LoadingContainer from '../../../components/design/loading-container';
import SearchFilter from './SearchFilter';

class ListPage extends React.Component {
    constructor(props) {
        super(props);

        this.loadInitialList = this.loadInitialList.bind(this);
    }

    componentDidMount() {
        this.loadInitialList();
    }

    componentDidUpdate({ match: prevMatch }) {
        const { match } = this.props;

        if (prevMatch.params.category !== match.params.category) {
            this.loadInitialList();
        }
    }

    loadInitialList() {
        const { load, match } = this.props;

        load(match.params.category);
    }

    render() {
        const {
            data,
            loading,
            match,
            sorting,
            ui,
        } = this.props;

        return (
            <LoadingContainer loading={loading}>
                <PageNavigation current={ui.title}>
                    <NavItem>
                        <Button tag={Link} to={`/${match.params.category}/edit`}>
                            +
                        </Button>
                    </NavItem>
                </PageNavigation>
                <SearchFilter />
                <LayoutGroup
                    content={ui.layout}
                    data={data}
                    sorting={sorting}
                />
            </LoadingContainer>
        );
    }
}

ListPage.propTypes = {
    data: PropTypes.shape({}).isRequired,
    load: PropTypes.func.isRequired,
    loading: PropTypes.bool.isRequired,
    match: PropTypes.shape({}).isRequired,
    ui: PropTypes.shape({
        layout: PropTypes.arrayOf(PropTypes.shape({

        })),
        title: PropTypes.string,
    }).isRequired,
    sorting: PropTypes.shape({
        column: PropTypes.string,
        direction: PropTypes.oneOf(['ASC', 'DESC']),
    }),
};

ListPage.defaultProps = {
    sorting: undefined,
};

const mapStateToProps = state => ({
    loading: state.listPage.loading,
    ui: state.listPage.ui,
    data: state.listPage.data,
    sorting: state.listPage.sorting,
});

const actions = {
    load: loadList,
};

export default connect(mapStateToProps, actions)(ListPage);
