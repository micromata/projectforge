import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';
import { loadList } from '../../../actions';
import Navigation from '../../../components/base/navigation';
import LayoutGroup from '../../../components/base/page/layout/Group';
import PageNavigation from '../../../components/base/page/Navigation';
import { Alert, Button, NavItem } from '../../../components/design';
import LoadingContainer from '../../../components/design/loading-container';
import { dataPropType } from '../../../utilities/propTypes';
import { getObjectFromQuery } from '../../../utilities/rest';
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
        const { load, location, match } = this.props;

        load(match.params.category, getObjectFromQuery(location.search || ''));
    }

    render() {
        const {
            data,
            error,
            loading,
            match,
            sorting,
            ui,
        } = this.props;

        console.log(ui.pageMenu);

        if (error === '404') {
            return (
                <Alert color="warning">
                    <h4>[Not found]</h4>
                </Alert>
            );
        }

        return (
            <LoadingContainer loading={loading}>
                <PageNavigation current={ui.title}>
                    <NavItem>
                        <Button tag={Link} to={`/${match.params.category}/edit`}>
                            +
                        </Button>
                    </NavItem>
                    <Navigation entries={ui.pageMenu || []} />
                </PageNavigation>
                <SearchFilter />
                <LayoutGroup
                    content={ui.layout}
                    data={data}
                    sorting={sorting}
                    translations={ui.translations}
                />
            </LoadingContainer>
        );
    }
}

ListPage.propTypes = {
    data: dataPropType.isRequired,
    load: PropTypes.func.isRequired,
    loading: PropTypes.bool.isRequired,
    location: PropTypes.shape({
        search: PropTypes.string,
    }).isRequired,
    match: PropTypes.shape({}).isRequired,
    ui: PropTypes.shape({
        layout: PropTypes.arrayOf(PropTypes.shape({

        })),
        title: PropTypes.string,
    }).isRequired,
    error: PropTypes.string,
    sorting: PropTypes.shape({
        column: PropTypes.string,
        direction: PropTypes.oneOf(['ASC', 'DESC']),
    }),
};

ListPage.defaultProps = {
    sorting: undefined,
    error: undefined,
};

const mapStateToProps = state => ({
    data: state.listPage.data,
    error: state.listPage.error,
    loading: state.listPage.loading,
    sorting: state.listPage.sorting,
    ui: state.listPage.ui,
});

const actions = {
    load: loadList,
};

export default connect(mapStateToProps, actions)(ListPage);
