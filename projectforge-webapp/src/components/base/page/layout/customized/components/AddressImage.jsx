import { faTrash } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { dataPropType } from '../../../../../../utilities/propTypes';
import { getServiceURL, handleHTTPErrors } from '../../../../../../utilities/rest';
import { Alert, Button } from '../../../../../design';
import DropArea from '../../../../../design/droparea';
import LoadingContainer from '../../../../../design/loading-container';
import style from './Customized.module.scss';

class CustomizedAddressImage extends Component {
    constructor(props) {
        super(props);

        const { data } = props;

        this.state = {
            loading: false,
            error: undefined,
            src: getServiceURL(`address/image/${data.id}?${new Date().getTime()}`),
        };

        this.handleFileChange = this.handleFileChange.bind(this);
        this.handleDeleteClick = this.handleDeleteClick.bind(this);
    }

    handleFileChange(files) {
        this.setState({
            loading: true,
            error: undefined,
        });

        const formData = new FormData();
        formData.append('file', files[0]);

        fetch(
            getServiceURL('address/uploadImage/-1'),
            {
                credentials: 'include',
                method: 'POST',
                body: formData,
            },
        )
            .then(handleHTTPErrors)
            .then(() => {
                const { changeDataField } = this.props;

                changeDataField('imageData', [1]);

                const fileReader = new FileReader();

                fileReader.onload = ({ currentTarget }) => this.setState({
                    src: currentTarget.result,
                    loading: false,
                });

                fileReader.readAsDataURL(files[0]);
            })
            .catch(error => this.setState({
                error,
                loading: false,
            }));
    }

    handleDeleteClick() {
        this.setState({
            loading: true,
            error: undefined,
        });

        fetch(
            getServiceURL('address/deleteImage/-1'),
            {
                credentials: 'include',
                method: 'DELETE',
            },
        )
            .then(handleHTTPErrors)
            .then(() => {
                const { changeDataField } = this.props;

                changeDataField('imageData', undefined);

                this.setState({ loading: false });
            })
            .catch(error => this.setState({
                error,
                loading: false,
            }));
    }

    render() {
        const { data, translation } = this.props;
        const { loading, error, src } = this.state;
        let image;

        if (data.imageData) {
            image = (
                <React.Fragment>
                    <img
                        className={style.addressImage}
                        src={src}
                        alt={`${data.firstName} ${data.name} (${data.organization})`}
                    />
                    <Button
                        onClick={this.handleDeleteClick}
                        color="danger"
                    >
                        <FontAwesomeIcon icon={faTrash} />
                        {` ${translation.delete}`}
                    </Button>
                </React.Fragment>
            );
        }

        return (
            <LoadingContainer loading={loading} className={style.addressImageContainer}>
                {error
                    ? <Alert color="danger">{translation['address.image.upload.error']}</Alert>
                    : undefined}
                {image}
                <DropArea setFiles={this.handleFileChange}>
                    {translation['file.upload.dropArea']}
                </DropArea>
            </LoadingContainer>
        );
    }
}

CustomizedAddressImage.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    data: dataPropType.isRequired,
    translation: PropTypes.shape({
        'address.image.upload.error': PropTypes.string,
        delete: PropTypes.string,
        'file.upload.dropArea': PropTypes.string,
    }).isRequired,
};

export default CustomizedAddressImage;
