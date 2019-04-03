import { faTrash } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React from 'react';
import { dataPropType } from '../../../../../../utilities/propTypes';
import { getServiceURL } from '../../../../../../utilities/rest';
import { Button } from '../../../../../design';
import DropArea from '../../../../../design/droparea';
import style from './Customized.module.scss';

function CustomizedAddressImage({ data }) {
    let image;

    if (data.imageData) {
        image = (
            <React.Fragment>
                <img
                    className={style.addressImage}
                    src={getServiceURL(`address/image/${data.id}`)}
                    alt={`${data.firstName} ${data.name} (${data.organization})`}
                />
                <Button>
                    <FontAwesomeIcon icon={faTrash} />
                </Button>
            </React.Fragment>
        );
    }

    return (
        <div className={style.addressImageContainer}>
            {image}
            <DropArea />
        </div>
    );
}

CustomizedAddressImage.propTypes = {
    data: dataPropType.isRequired,
};

export default CustomizedAddressImage;
