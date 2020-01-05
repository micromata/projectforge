import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import AdvancedPopper from '../../../../components/design/popper/AdvancedPopper';
import AdvancedPopperAction from '../../../../components/design/popper/AdvancedPopperAction';
import styles from '../ListPage.module.scss';

function MagicFilterPill(
    {
        children,
        hasValue,
        name,
        translations,
        ...props
    },
) {
    const [isOpen, setIsOpen] = React.useState(false);

    return (
        <div className={styles.magicFilter}>
            <AdvancedPopper
                setIsOpen={setIsOpen}
                isOpen={isOpen}
                basic={name}
                contentClassName={classNames(styles.pill, { [styles.marked]: isOpen || hasValue })}
                actions={(
                    <React.Fragment>
                        <AdvancedPopperAction type="delete" disabled={!hasValue}>
                            {translations.delete || ''}
                        </AdvancedPopperAction>
                        <AdvancedPopperAction type="success">
                            {translations.save || ''}
                        </AdvancedPopperAction>
                    </React.Fragment>
                )}
                {...props}
            >
                {children}
            </AdvancedPopper>
        </div>
    );
}

MagicFilterPill.propTypes = {
    name: PropTypes.string.isRequired,
    translations: PropTypes.shape({}).isRequired,
    children: PropTypes.node,
    hasValue: PropTypes.bool,
};

MagicFilterPill.defaultProps = {
    children: undefined,
    hasValue: false,
};

const mapStateToProps = ({ list }) => ({
    translations: list.categories[list.currentCategory].ui.translations,
});

export default connect(mapStateToProps)(MagicFilterPill);
