import React from 'react';
import { Progress } from 'reactstrap';
import { FileHeader } from './FileHeader';

export interface UploadErrorProps {
    file: File;
    error: string;
}

/*
 * Thanks to: https://github.com/bmvantunes/youtube-2021-feb-multiple-file-upload-formik/blob/main/src/upload/UploadError.tsx
 */

export function UploadError({ file, error }: UploadErrorProps) {
    return (
        <>
            <FileHeader file={file} />
            <Progress color="danger" value={100}>
                {error}
            </Progress>
        </>
    );
}
