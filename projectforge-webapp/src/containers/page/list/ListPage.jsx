import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { loadList } from '../../../actions';
import DynamicLayout from '../../../components/base/dynamicLayout';
import { Card, Container } from '../../../components/design';
import SearchFilter from './searchFilter/SearchFilter';
import styles from './ListPage.module.scss';

function ListPage(
    {
        category,
        location,
        match,
        onCategoryChange,
    },
) {
    // Only reload the list when the category or search string changes.
    React.useEffect(
        () => {
            onCategoryChange(match.params.category, true, (location.state || {}).variables);
        },
        [match.params.category, location.search, location.state],
    );

    // TODO ADD ERROR HANDLING

    return (
        <Container fluid>
            <Card>
                {category && (
                    <DynamicLayout
                        ui={category.ui}
                        data={category.data}
                        setData={undefined}
                        options={{
                            displayPageMenu: false,
                            setBrowserTitle: true,
                            showActionButtons: false,
                        }}
                        variables={category.variables}
                    >
                        <h4 className={styles.uiTitle}>{category.ui.title}</h4>
                        {!category.ui.hideSearchFilter && (
                            <SearchFilter />
                        )}
                    </DynamicLayout>
                )}
            </Card>
        </Container>
    );
}

ListPage.propTypes = {
    location: PropTypes.shape({
        search: PropTypes.string,
        state: PropTypes.shape({
            id: PropTypes.number,
        }),
    }).isRequired,
    match: PropTypes.shape({
        params: PropTypes.shape({
            category: PropTypes.string.isRequired,
        }).isRequired,
    }).isRequired,
    onCategoryChange: PropTypes.func.isRequired,
    category: PropTypes.shape({
        ui: PropTypes.shape({
            title: PropTypes.string,
            hideSearchFilter: PropTypes.bool,
        }),
        data: PropTypes.shape({ }),
        variables: PropTypes.shape({ }),
    }),
};

ListPage.defaultProps = {
    category: undefined,
};

const mapStateToProps = ({ list }, { match }) => ({
    category: list.categories[match.params.category],
});

const actions = {
    onCategoryChange: loadList,
};

export default connect(mapStateToProps, actions)(ListPage);
