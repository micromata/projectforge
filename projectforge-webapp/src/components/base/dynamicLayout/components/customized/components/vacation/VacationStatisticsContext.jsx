import React from 'react';

export const defaultValues = {
    current: {},
    prev: {},
    translations: {},
};

export const VacationStatisticsContext = React.createContext(defaultValues);
