import { faTimes, faTrash } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import AdvancedPopperInput from '../../popper/AdvancedPopperInput';
import styles from './AutoCompletion.module.scss';
import AutoCompletion from './index';

function ObjectAutoCompletion(
    {
        inputId,
        inputProps,
        onSelect,
        value,
        ...props
    },
) {
    const [search, setSearch] = React.useState('');

    React.useEffect(() => {
        if (search !== value.displayName) {
            setSearch(value.displayName);
        }
    }, [value.displayName]);

    const handleChange = ({ target }) => setSearch(target.value);
    const handleBlur = () => {
        if (search === '') {
            onSelect(null);
            return;
        }

        if (search !== value.displayName) {
            setSearch(value.displayName);
        }
    };

    const handleDelete = (event) => {
        event.stopPropagation();

        setSearch('');
        onSelect(null);
    };

    return (
        <AutoCompletion
            input={({ ref, ...otherInputsProps }) => (
                <AdvancedPopperInput
                    forwardRef={ref}
                    id={inputId}
                    {...otherInputsProps}
                    {...inputProps}
                    onBlur={handleBlur}
                    onChange={handleChange}
                >
                    {search !== '' && (
                        <FontAwesomeIcon
                            onClick={handleDelete}
                            icon={faTimes}
                            className={styles.deleteIcon}
                        />
                    )}
                </AdvancedPopperInput>
            )}
            onSelect={onSelect}
            search={search}
            {...props}
        />
    );
}

ObjectAutoCompletion.propTypes = {
    inputId: PropTypes.string.isRequired,
    onSelect: PropTypes.func.isRequired,
    icon: PropTypes.shape({}),
    inputProps: PropTypes.shape({}),
    value: PropTypes.shape({}),
};

ObjectAutoCompletion.defaultProps = {
    icon: undefined,
    inputProps: undefined,
    value: {},
};

export default ObjectAutoCompletion;
