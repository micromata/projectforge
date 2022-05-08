import React from 'react';

/*
* Thanks to: https://github.com/bmvantunes/youtube-2021-feb-multiple-file-upload-formik/blob/main/src/upload/FileHeader.tsx
*/
export interface FileHeaderProps {
    file: File;
    onDelete: (file: File) => void;
}

export function FileHeader({ file }: FileHeaderProps) {
    return <div>{file.name}</div>;
    /* return (
         <Grid container justify="space-between" alignItems="center">
             <Grid item>{file.name}</Grid>
             <Grid item>
                 <Button size="small" onClick={() => onDelete(file)}>
                     Delete
                 </Button>
             </Grid>
         </Grid>
     ); */
}
