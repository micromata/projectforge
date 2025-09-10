import { faTimes } from '@fortawesome/free-solid-svg-icons';
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
        urlparams,
        value = {},
        ...props
    },
) {
    const displayName = value ? value.displayName : '';
    const [search, setSearch] = React.useState(displayName);

    React.useEffect(() => {
        if (search !== displayName) {
            setSearch(displayName);
        }
    }, [displayName]);

    const handleChange = ({ target }) => setSearch(target.value);
    const handleBlur = () => {
        if (search === '') {
            onSelect(null);
            return;
        }

        if (search !== displayName) {
            setSearch(displayName);
        }
    };

    const handleDelete = (event) => {
        event.stopPropagation();

        setSearch('');
        onSelect(null);
    };

    return (
        <AutoCompletion
            /* eslint-disable-next-line react/no-unstable-nested-components */
            input={({ ref, ...otherInputsProps }) => (
                <AdvancedPopperInput
                    forwardRef={ref}
                    id={inputId}
                    {...otherInputsProps}
                    {...inputProps}
                    onBlur={handleBlur}
                    onChange={handleChange}
                >
                    {search && (
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
            urlparams={urlparams}
            {...props}
        />
    );
}

ObjectAutoCompletion.propTypes = {
    inputId: PropTypes.string.isRequired,
    onSelect: PropTypes.func.isRequired,
    icon: PropTypes.shape({}),
    inputProps: PropTypes.shape({}),
    urlparams: PropTypes.shape({}),
    value: PropTypes.shape({
        displayName: PropTypes.string,
    }),
};

export default ObjectAutoCompletion;
