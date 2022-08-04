import { faCheck, faStar } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import moment from 'moment';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import formatterFormat from './FormatterFormat';
import TreeNavigation from '../../containers/panel/task/TreeNavigation';
import ConsumptionBar from '../../containers/panel/task/ConsumptionBar';

function Formatter(
    {
        formatter,
        value,
        data,
        id,
        dataType,
        dateFormat,
        timestampFormatSeconds,
        timestampFormatMinutes,
        valueIconMap,
        locale,
        currency,
    },
) {
    const useValue = value || Object.getByString(data, id);
    if (useValue === undefined) {
        return null;
    }
    let result = useValue;
    const valueIconsPresent = valueIconMap && valueIconMap.length !== 0;
    const useFormatter = !valueIconsPresent && (formatter || dataType);

    if (useFormatter) {
        switch (useFormatter) {
            case 'BOOLEAN':
                if (useValue) {
                    result = <FontAwesomeIcon icon={faCheck} />;
                }
                break;
            case 'RATING':
                if (useValue > 0) {
                    result = [...Array(useValue).keys()].map((v) => (
                        <FontAwesomeIcon
                            icon={faStar}
                            color="#ffc107"
                            key={v}
                        />
                    ));
                } else {
                    result = '-';
                }
                break;
            case 'TREE_NAVIGATION':
                result = (
                    <TreeNavigation
                        treeStatus={data.treeStatus}
                        id={id}
                        indent={data.indent}
                        title={useValue}
                    />
                );
                break;
            case 'CONSUMPTION':
                result = (
                    <ConsumptionBar
                        progress={data.consumption}
                        identifier={id}
                    />
                );
                break;
            default:
                result = formatterFormat(
                    useValue,
                    useFormatter,
                    dateFormat,
                    timestampFormatSeconds,
                    timestampFormatMinutes,
                    locale,
                    currency,
                );
        }
    } else if (dataType === 'DATE') {
        result = moment(useValue)
            .format(timestampFormatMinutes);
    } else if (valueIconsPresent) {
        const valueIcon = valueIconMap[useValue];

        if (valueIcon) {
            result = <FontAwesomeIcon icon={valueIcon} />;
        }
    }

    if (result === undefined) {
        result = '???';
    }

    return result;
}

Formatter.propTypes = {
    // eslint-disable-next-line react/forbid-prop-types
    data: PropTypes.any,
    // eslint-disable-next-line react/forbid-prop-types
    value: PropTypes.any,
    dataType: PropTypes.string,
    dateFormat: PropTypes.string,
    id: PropTypes.string,
    formatter: PropTypes.string,
    timestampFormatSeconds: PropTypes.string,
    timestampFormatMinutes: PropTypes.string,
    locale: PropTypes.string,
    currency: PropTypes.string,
    valueIconMap: PropTypes.shape({
        length: PropTypes.number,
    }),
};

Formatter.defaultProps = {
    data: undefined,
    id: undefined,
    value: undefined,
    formatter: undefined,
    dataType: undefined,
    dateFormat: 'DD/MM/YYYY',
    timestampFormatSeconds: 'DD.MM.YYYY HH:mm:ss',
    timestampFormatMinutes: 'DD.MM.YYYY HH:mm',
    locale: undefined,
    currency: undefined,
    valueIconMap: undefined,
};

const mapStateToProps = ({ authentication }) => ({
    dateFormat: authentication.user.jsDateFormat,
    timestampFormatSeconds: authentication.user.jsTimestampFormatSeconds,
    timestampFormatMinutes: authentication.user.jsTimestampFormatMinutes,
});

export default connect(mapStateToProps)(Formatter);
