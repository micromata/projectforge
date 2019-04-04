import { faTrash } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { dataPropType } from '../../../../../../utilities/propTypes';
import { getServiceURL } from '../../../../../../utilities/rest';
import { Button } from '../../../../../design';
import DropArea from '../../../../../design/droparea';
import style from './Customized.module.scss';

function CustomizedAddressImage({ changeDataField, data, id }) {
    let image;

    if (data.imageData) {
        image = (
            <React.Fragment>
                <img
                    className={style.addressImage}
                    src={getServiceURL(`address/image/${data.id}`)}
                    alt={`${data.firstName} ${data.name} (${data.organization})`}
                />
                <Button
                    onClick={() => changeDataField(id, undefined)}
                    color="danger"
                >
                    <FontAwesomeIcon icon={faTrash} />
                    [Löschen]
                </Button>
            </React.Fragment>
        );
    }

    return (
        <div className={style.addressImageContainer}>
            {image}
            <DropArea
                setFiles={files => changeDataField(id, files[0])}
            />
        </div>
    );
}

CustomizedAddressImage.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    data: dataPropType.isRequired,
    id: PropTypes.string.isRequired,
};

export default CustomizedAddressImage;
