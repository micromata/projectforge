import { faTrash } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React from 'react';
import { getServiceURL, handleHTTPErrors } from '../../../../../../utilities/rest';
import { Alert, Button } from '../../../../../design';
import DropArea from '../../../../../design/droparea';
import LoadingContainer from '../../../../../design/loading-container';
import { DynamicLayoutContext } from '../../../context';
import style from './Customized.module.scss';

function CustomizedAddressImage() {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const [loading, setLoading] = React.useState(false);
    const [error, setError] = React.useState(undefined);
    const [src, setSrc] = React.useState('');

    React.useEffect(() => {
        // Priority 1: Use Data URL if available (from VCF import or manual upload via FileReader)
        if (data.imageDataUrl) {
            setSrc(data.imageDataUrl);
        } else if (data.id !== undefined) {
            // Priority 2: Load from DB for existing addresses
            setSrc(getServiceURL(`address/image/${data.id}?${new Date().getTime()}`));
        } else if (data.hasUploadedImage) {
            // Priority 3: Load from session for new addresses with uploaded image
            setSrc(getServiceURL(`address/image/-1?${new Date().getTime()}`));
        }
    }, [data.id, data.hasUploadedImage, data.imageData, data.imageDataUrl]);

    return React.useMemo(
        () => {
            let image;

            const changeFile = (files) => {
                setLoading(true);
                setError(undefined);

                const formData = new FormData();
                formData.append('file', files[0]);

                fetch(
                    // Set the image with id -1, so the image will be set in the session.
                    getServiceURL('address/uploadImage/-1'),
                    {
                        credentials: 'include',
                        method: 'POST',
                        body: formData,
                    },
                )
                    .then(handleHTTPErrors)
                    .then(() => {
                        const fileReader = new FileReader();

                        fileReader.onload = ({ currentTarget }) => {
                            setData({
                                hasUploadedImage: true,
                                imageDataUrl: currentTarget.result,
                            });
                            setLoading(false);
                        };

                        fileReader.readAsDataURL(files[0]);
                    })
                    .catch((catchError) => {
                        setError(catchError);
                        setLoading(false);
                    });
            };

            const deleteImage = () => {
                setLoading(true);
                setError(undefined);

                fetch(
                    // Delete the image with id -1, so the stored image in the session will be
                    // removed.
                    getServiceURL(`address/deleteImage/${data.id || -1}`),
                    {
                        credentials: 'include',
                        method: 'DELETE',
                    },
                )
                    .then(handleHTTPErrors)
                    .then(() => setData({
                        imageData: undefined,
                        hasUploadedImage: false,
                        imageDataUrl: undefined,
                    }))
                    .catch((fetchError) => setError(fetchError))
                    .finally(() => setLoading(false));
            };

            if (data.imageData || data.hasUploadedImage) {
                image = (
                    <>
                        <img
                            className={style.addressImage}
                            src={src}
                            alt={`${data.firstName} ${data.name} (${data.organization})`}
                        />
                        <Button
                            onClick={deleteImage}
                            color="danger"
                        >
                            <FontAwesomeIcon icon={faTrash} />
                            {` ${ui.translations.delete}`}
                        </Button>
                    </>
                );
            }

            return (
                <LoadingContainer
                    loading={loading}
                    className={style.addressImageContainer}
                >
                    {error
                        ? (
                            <Alert color="danger">
                                {ui.translations['address.image.upload.error']}
                            </Alert>
                        )
                        : undefined}
                    {image}
                    <DropArea
                        setFiles={changeFile}
                        title={ui.translations['file.upload.dropArea']}
                    />
                </LoadingContainer>
            );
        },
        [
            data.imageData,
            data.hasUploadedImage,
            data.firstName,
            data.name,
            data.organization,
            setData,
            loading,
            src,
        ],
    );
}

CustomizedAddressImage.propTypes = {};

export default CustomizedAddressImage;
