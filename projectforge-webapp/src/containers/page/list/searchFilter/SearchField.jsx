import { faSearch } from '@fortawesome/free-solid-svg-icons';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Input } from '../../../../components/design';
import styles from '../ListPage.module.scss';

function SearchField(
    {
        // Extract 'dispatch' so it's not passed to the input tag
        dispatch,
        ...props
    },
) {
    return (
        <Input
            icon={faSearch}
            className={styles.search}
            autoComplete="off"
            {...props}
        />
    );
}

SearchField.propTypes = {
    id: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    dispatch: PropTypes.func,
    placeholder: PropTypes.string,
    value: PropTypes.string,
};

SearchField.defaultProps = {
    dispatch: undefined,
    placeholder: '',
    value: '',
};

const mapStateToProps = ({ list }) => ({
    placeholder: list.categories[list.currentCategory].ui.translations.search,
});

export default connect(mapStateToProps)(SearchField);
