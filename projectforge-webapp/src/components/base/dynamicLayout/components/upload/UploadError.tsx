import React from 'react';
import { Progress } from 'reactstrap';
import { FileError } from 'react-dropzone';
import { FileHeader } from './FileHeader';

export interface UploadErrorProps {
    file: File;
    errors: FileError[];
}

/*
 * Thanks to: https://github.com/bmvantunes/youtube-2021-feb-multiple-file-upload-formik/blob/main/src/upload/UploadError.tsx
 */

export function UploadError({ file, errors }: UploadErrorProps) {
    return (
        <>
            <FileHeader file={file} />
            <Progress color="danger" value={100}>
                {errors.map((error) => (
                    <div key={error.code}>
                        {error.message}
                    </div>
                ))}
                ;
            </Progress>
        </>
    );
}
