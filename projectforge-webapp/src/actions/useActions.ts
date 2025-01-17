import { useDispatch } from 'react-redux';
import { useMemo } from 'react';
import { ActionCreator, bindActionCreators } from 'redux';

function useActions<C extends ActionCreator<any>, A extends C | C[]>(
    actions: A,
    deps?: readonly unknown[],
) {
    const dispatch = useDispatch();
    return useMemo(
        () => {
            if (Array.isArray(actions)) {
                return actions.map((a) => bindActionCreators(a, dispatch));
            }
            return bindActionCreators(actions, dispatch);
        },
        deps ? [dispatch, ...deps] : [dispatch],
    ) as A extends any[] ? C[] : C;
}

export default useActions;
