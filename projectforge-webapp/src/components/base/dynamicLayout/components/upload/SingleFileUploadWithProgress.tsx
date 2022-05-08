import React, { useEffect, useState } from 'react';
import { Progress } from 'reactstrap';
import { FileHeader } from './FileHeader';

/*
 * Thanks to: https://github.com/bmvantunes/youtube-2021-feb-multiple-file-upload-formik/blob/main/src/upload/SingleFileUploadWithProgress.tsx
 */

export interface SingleFileUploadWithProgressProps {
    file: File;
    url: string;
    onDelete: (file: File) => void;
    onUpload: (file: File, url: string) => void;
}

function uploadFile(file: File, url: string, onProgress: (percentage: number) => void) {
    const key = 'docs_upload_example_us_preset';

    return new Promise<string>((res, rej) => {
        const xhr = new XMLHttpRequest();
        xhr.open('POST', url);
        xhr.withCredentials = true;

        xhr.onload = () => {
            const resp = JSON.parse(xhr.responseText);
            res(resp.secure_url);
        };
        xhr.onerror = (evt) => rej(evt);
        xhr.upload.onprogress = (event) => {
            if (event.lengthComputable) {
                const percentage = (event.loaded / event.total) * 100;
                onProgress(Math.round(percentage));
            }
        };

        const formData = new FormData();
        formData.append('file', file);
        formData.append('upload_preset', key);

        xhr.send(formData);
    });
}

export function SingleFileUploadWithProgress({
    file,
    url,
    onDelete,
    onUpload,
}: SingleFileUploadWithProgressProps) {
    const [progress, setProgress] = useState(0);

    useEffect(() => {
        async function upload() {
            await uploadFile(file, url, setProgress);
            onUpload(file, url);
        }

        upload();
    }, []);

    return (
        <div>
            <FileHeader file={file} onDelete={onDelete} />
            <Progress
                animated
                color="warning"
                value={progress}
            />
        </div>
    );
}
