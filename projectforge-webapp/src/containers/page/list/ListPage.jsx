import PropTypes from 'prop-types';
import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import { connect } from 'react-redux';
import { useLocation, useParams } from 'react-router';
import { callAction, loadList } from '../../../actions';
import DynamicLayout from '../../../components/base/dynamicLayout';
import { Card, Container } from '../../../components/design';
import SearchFilter from './searchFilter/SearchFilter';
import styles from './ListPage.module.scss';

function ListPage(
    {
        category,
        onCallAction,
        onCategoryChange,
    },
) {
    const { category: paramsCategory } = useParams();
    const location = useLocation();

    // Only reload the list when the category or search string changes.
    React.useEffect(
        () => {
            onCategoryChange(paramsCategory, true, (location.state || {}).variables);
        },
        [paramsCategory, location.search, location.state],
    );

    // TODO ADD ERROR HANDLING

    return (
        <Container fluid>
            <Card>
                {category && (
                    <DynamicLayout
                        callAction={onCallAction}
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
                        <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeRaw]}>
                            {category.data.resultInfo}
                        </ReactMarkdown>
                    </DynamicLayout>
                )}
            </Card>
        </Container>
    );
}

ListPage.propTypes = {
    onCallAction: PropTypes.func.isRequired,
    onCategoryChange: PropTypes.func.isRequired,
    category: PropTypes.shape({
        ui: PropTypes.shape({
            title: PropTypes.string,
            hideSearchFilter: PropTypes.bool,
            pageMenu: PropTypes.arrayOf(PropTypes.shape({})),
        }),
        data: PropTypes.shape({
            resultInfo: PropTypes.shape({}),
        }),
        variables: PropTypes.shape({ }),
    }),
};

const mapStateToProps = ({ list }, { match }) => ({
    category: list.categories[match.params.category],
});

const actions = {
    onCallAction: callAction,
    onCategoryChange: loadList,
};

export default connect(mapStateToProps, actions)(ListPage);
