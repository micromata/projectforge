import AwesomeDebouncePromise from 'awesome-debounce-promise';
import PropTypes from 'prop-types';
import React from 'react';
import { debouncedWaitTime, getServiceURL, handleHTTPErrors } from '../../../../utilities/rest';
import AdvancedPopper from '../../popper/AdvancedPopper';
import styles from './AutoCompletion.module.scss';
import Completion from './Completion';

const loadCompletionsBounced = (
    {
        url,
        search = '',
        setCompletions,
        searchParameter,
    },
) => {
    fetch(
        getServiceURL(url.replace(`:${searchParameter}`, encodeURIComponent(search))),
        {
            method: 'GET',
            credentials: 'include',
            headers: { Accept: 'application/json' },
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(setCompletions)
        .catch(() => setCompletions([]));
};

function AutoCompletion(
    {
        input,
        url,
        onSelect,
        search,
        searchParameter,
        ...props
    },
) {
    const [completions, setCompletions] = React.useState([]);
    const [isOpen, setIsOpen] = React.useState(false);
    const searchRef = React.useRef(null);
    const [loadCompletions] = React.useState(
        () => AwesomeDebouncePromise(loadCompletionsBounced, debouncedWaitTime),
    );

    const handleKeyDown = ({ key }) => {
        if (key === 'Escape' || key === 'Enter') {
            if (searchRef.current) {
                searchRef.current.blur();
            }
            setIsOpen(false);
        }
    };

    React.useEffect(() => {
        if (url) {
            loadCompletions({
                url,
                search,
                setCompletions,
                searchParameter,
            });
        }
    }, [url, search]);

    return (
        <AdvancedPopper
            additionalClassName={styles.completions}
            basic={input({
                ref: searchRef,
                onKeyDown: handleKeyDown,
                value: search,
            })}
            setIsOpen={setIsOpen}
            isOpen={isOpen && completions.length !== 0}
            {...props}
        >
            <ul className={styles.entries}>
                {completions.map(completion => (
                    <Completion
                        key={`completion-${completion.id}`}
                        displayName={completion.displayName}
                        onClick={() => onSelect(completion)}
                    />
                ))}
            </ul>
        </AdvancedPopper>
    );
}

AutoCompletion.propTypes = {
    input: PropTypes.func.isRequired,
    onSelect: PropTypes.func.isRequired,
    url: PropTypes.string,
    search: PropTypes.string,
    searchParameter: PropTypes.string,
};

AutoCompletion.defaultProps = {
    url: undefined,
    search: '',
    searchParameter: 'searchString',
};

export default AutoCompletion;
