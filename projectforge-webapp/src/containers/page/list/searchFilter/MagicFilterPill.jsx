import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import AdvancedPopper from '../../../../components/design/popper/AdvancedPopper';
import AdvancedPopperAction from '../../../../components/design/popper/AdvancedPopperAction';
import styles from '../ListPage.module.scss';

function MagicFilterPill(
    {
        children,
        name,
        translations,
        value,
    },
) {
    const [isOpen, setIsOpen] = React.useState(false);

    return (
        <div className={styles.magicFilter}>
            <AdvancedPopper
                setIsOpen={setIsOpen}
                isOpen={isOpen}
                basic={name}
                contentClassName={classNames(styles.pill, { [styles.isOpen]: isOpen || value })}
                actions={(
                    <React.Fragment>
                        <AdvancedPopperAction type="delete" disabled>
                            {translations.delete || ''}
                        </AdvancedPopperAction>
                        <AdvancedPopperAction type="success">
                            {translations.save || ''}
                        </AdvancedPopperAction>
                    </React.Fragment>
                )}
                additionalClassName={styles.addition}
            >
                {children}
            </AdvancedPopper>
        </div>
    );
}

MagicFilterPill.propTypes = {
    children: PropTypes.node.isRequired,
    name: PropTypes.string.isRequired,
    translations: PropTypes.shape({}).isRequired,
    value: PropTypes.string,
};

MagicFilterPill.defaultProps = {
    value: undefined,
};

export default MagicFilterPill;
