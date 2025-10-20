import { faBan } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { UncontrolledTooltip } from 'reactstrap';
import { removeFilter, setFilter } from '../../../../../actions/list/filter';
import AdvancedPopper from '../../../../../components/design/popper/AdvancedPopper';
import AdvancedPopperAction from '../../../../../components/design/popper/AdvancedPopperAction';
import styles from '../../ListPage.module.scss';
import useMagicInput from './inputs/MagicInputHook';

function MagicFilterPill(
    {
        id,
        isNew = false,
        isRemovable = false,
        label,
        onFilterDelete,
        onFilterSet,
        tooltip,
        translations,
        filterType,
        value = {},
        ...props
    },
) {
    const [isOpen, setIsOpen] = React.useState(isNew);
    const [tempValue, setTempValue] = React.useState({});

    const MagicInput = useMagicInput(filterType);

    React.useEffect(() => {
        if (Object.isEmpty(value) && MagicInput.defaultValue !== undefined) {
            setTempValue(MagicInput.defaultValue);
        } else {
            setTempValue(value);
        }
    }, []);

    const handleCancel = () => {
        setTempValue(value);
        setIsOpen(false);
    };

    const handleDelete = () => {
        setIsOpen(false);
        onFilterDelete(id);
    };

    const handleSave = () => {
        setIsOpen(false);

        if (MagicInput.isEmpty(tempValue)) {
            handleDelete();
            return;
        }

        onFilterSet(id, tempValue);
    };

    const tooltipId = `magic-filter-${id}`;

    return (
        <div className={styles.magicFilter}>
            <AdvancedPopper
                setIsOpen={setIsOpen}
                isOpen={isOpen}
                id={tooltipId}
                basic={(
                    <>
                        {value && Object.keys(value).length
                            ? MagicInput.getLabel(label, value, props)
                            : label}
                        {(isRemovable || !Object.isEmpty(value)) && (
                            <FontAwesomeIcon
                                icon={faBan}
                                className={styles.deleteIcon}
                                onClick={() => onFilterDelete(id)}
                            />
                        )}
                    </>
                )}
                contentClassName={classNames(
                    styles.pill,
                    { [styles.marked]: isOpen || !Object.isEmpty(value) },
                )}
                actions={(
                    <>
                        <AdvancedPopperAction
                            type="delete"
                            disabled={!value}
                            onClick={handleDelete}
                        >
                            {translations.delete || 'Delete'}
                        </AdvancedPopperAction>
                        <AdvancedPopperAction
                            type="success"
                            onClick={handleSave}
                        >
                            {translations.save || 'Save'}
                        </AdvancedPopperAction>
                    </>
                )}
            >
                <p className={styles.title}>{label}</p>
                <div className={styles.content}>
                    <MagicInput
                        label={label}
                        filterType={filterType}
                        id={id}
                        onChange={setTempValue}
                        onSubmit={handleSave}
                        onCancel={handleCancel}
                        value={tempValue}
                        translations={translations}
                        {...props}
                    />
                </div>
            </AdvancedPopper>
            {tooltip && (
                <UncontrolledTooltip
                    placement="auto"
                    target={tooltipId}
                    delay={{ show: 500, hide: 0 }}
                >
                    {tooltip}
                </UncontrolledTooltip>
            )}
        </div>
    );
}

MagicFilterPill.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    onFilterDelete: PropTypes.func.isRequired,
    onFilterSet: PropTypes.func.isRequired,
    tooltip: PropTypes.string,
    translations: PropTypes.shape({
        delete: PropTypes.string,
        save: PropTypes.string,
    }).isRequired,
    isNew: PropTypes.bool,
    isRemovable: PropTypes.bool,
    filterType: PropTypes.string,
    value: PropTypes.shape({}),
};

const mapStateToProps = ({ list }) => ({
    translations: list.categories[list.currentCategory].ui.translations,
});

const actions = (dispatch) => ({
    onFilterDelete: (fieldId) => dispatch(removeFilter(fieldId)),
    onFilterSet: (fieldId, newValue) => dispatch(setFilter(fieldId, newValue)),
});

export default connect(mapStateToProps, actions)(MagicFilterPill);
