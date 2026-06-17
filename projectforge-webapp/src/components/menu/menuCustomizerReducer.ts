import { arrayMove } from '@dnd-kit/sortable';
import { MenuCustomizerState, MenuAction, MenuItem } from './menuCustomizerTypes';
import { getItemId, isDuplicate } from './menuCustomizerUtils';

export const initialState: MenuCustomizerState = {
  loading: true,
  mainMenuStructured: [],
  mainMenuFlat: [],
  customMenu: [],
  lastSavedMenu: [],
  translations: {},
  activeId: null,
  draggedItem: null,
  error: null,
  success: null,
  isDirty: false,
  editingGroupId: null,
  showGroupInput: false,
  newGroupName: '',
};

export function menuCustomizerReducer(state: MenuCustomizerState, action: MenuAction): MenuCustomizerState {
  switch (action.type) {
    case 'LOAD_START':
      return { ...state, loading: true, error: null };

    case 'LOAD_SUCCESS': {
      const { mainMenuStructured, mainMenuFlat, customMenu, translations } = action.payload;
      return {
        ...state,
        loading: false,
        mainMenuStructured,
        mainMenuFlat,
        customMenu,
        lastSavedMenu: customMenu.map(item => ({ ...item, subMenu: item.subMenu ? [...item.subMenu] : undefined })),
        translations,
        isDirty: false,
        error: null,
      };
    }

    case 'LOAD_FAILURE':
      return { ...state, loading: false, error: action.payload };

    case 'DRAG_START':
      return { ...state, activeId: action.payload.id, draggedItem: action.payload.item };

    case 'DRAG_END':
      return { ...state, activeId: null, draggedItem: null };

    case 'REORDER': {
      const { container, oldIndex, newIndex } = action.payload;
      if (oldIndex === newIndex) return state;

      if (container === 'favorites') {
        return {
          ...state,
          customMenu: arrayMove(state.customMenu, oldIndex, newIndex),
          isDirty: true,
        };
      }

      // Reorder within a group
      const groupIndex = state.customMenu.findIndex(item => getItemId(item) === container);
      if (groupIndex === -1 || !state.customMenu[groupIndex].subMenu) return state;

      const newMenu = [...state.customMenu];
      newMenu[groupIndex] = {
        ...newMenu[groupIndex],
        subMenu: arrayMove(newMenu[groupIndex].subMenu!, oldIndex, newIndex),
      };
      return { ...state, customMenu: newMenu, isDirty: true };
    }

    case 'ADD_FROM_TEMPLATE': {
      const { item, targetContainer, targetIndex } = action.payload;
      const itemId = getItemId(item);

      if (isDuplicate(state.customMenu, itemId)) return state;

      const itemCopy = { ...item };
      const newMenu = [...state.customMenu];

      if (targetContainer === 'favorites') {
        newMenu.splice(targetIndex, 0, itemCopy);
      } else {
        const groupIndex = newMenu.findIndex(g => getItemId(g) === targetContainer);
        if (groupIndex === -1) return state;
        const subMenu = [...(newMenu[groupIndex].subMenu || [])];
        subMenu.splice(targetIndex, 0, itemCopy);
        newMenu[groupIndex] = { ...newMenu[groupIndex], subMenu };
      }

      return { ...state, customMenu: newMenu, isDirty: true };
    }

    case 'MOVE_TO_CONTAINER': {
      const { item, sourceContainer, sourceIndex, targetContainer, targetIndex } = action.payload;
      const itemId = getItemId(item);
      const newMenu = [...state.customMenu];

      // Remove from source
      if (sourceContainer === 'favorites') {
        newMenu.splice(sourceIndex, 1);
      } else {
        const srcGroupIdx = newMenu.findIndex(g => getItemId(g) === sourceContainer);
        if (srcGroupIdx !== -1 && newMenu[srcGroupIdx].subMenu) {
          const subMenu = [...newMenu[srcGroupIdx].subMenu!];
          subMenu.splice(sourceIndex, 1);
          newMenu[srcGroupIdx] = { ...newMenu[srcGroupIdx], subMenu };
        }
      }

      // Check for duplicate after removal
      if (isDuplicate(newMenu, itemId)) return state;

      // Add to target
      if (targetContainer === 'favorites') {
        newMenu.splice(targetIndex, 0, { ...item });
      } else {
        const destGroupIdx = newMenu.findIndex(g => getItemId(g) === targetContainer);
        if (destGroupIdx === -1) return state;
        const subMenu = [...(newMenu[destGroupIdx].subMenu || [])];
        subMenu.splice(targetIndex, 0, { ...item });
        newMenu[destGroupIdx] = { ...newMenu[destGroupIdx], subMenu };
      }

      return { ...state, customMenu: newMenu, isDirty: true };
    }

    case 'REMOVE_ITEM': {
      const { itemId, groupId } = action.payload;
      let newMenu: MenuItem[];

      if (groupId) {
        newMenu = state.customMenu.map(item => {
          if (getItemId(item) === groupId && item.subMenu) {
            return { ...item, subMenu: item.subMenu.filter(sub => getItemId(sub) !== itemId) };
          }
          return item;
        });
      } else {
        newMenu = state.customMenu.filter(item => getItemId(item) !== itemId);
      }

      return { ...state, customMenu: newMenu, isDirty: true };
    }

    case 'ADD_GROUP': {
      const groupId = `custom_group_${Date.now()}`;
      const newGroup: MenuItem = { id: groupId, title: action.payload.name, subMenu: [] };
      return {
        ...state,
        customMenu: [...state.customMenu, newGroup],
        showGroupInput: false,
        newGroupName: '',
        isDirty: true,
        error: null,
      };
    }

    case 'RENAME_GROUP': {
      const { groupId, name } = action.payload;
      const newMenu = state.customMenu.map(item =>
        getItemId(item) === groupId ? { ...item, title: name } : item
      );
      return { ...state, customMenu: newMenu, editingGroupId: null, isDirty: true, error: null };
    }

    case 'SAVE_START':
      return { ...state, loading: true };

    case 'SAVE_SUCCESS':
      return {
        ...state,
        loading: false,
        lastSavedMenu: state.customMenu.map(item => ({ ...item, subMenu: item.subMenu ? [...item.subMenu] : undefined })),
        isDirty: false,
        success: state.translations.menuSavedSuccessfully || 'Menu saved successfully',
      };

    case 'SAVE_FAILURE':
      return { ...state, loading: false, error: action.payload };

    case 'UNDO':
      return {
        ...state,
        customMenu: state.lastSavedMenu.map(item => ({ ...item, subMenu: item.subMenu ? [...item.subMenu] : undefined })),
        isDirty: false,
        success: state.translations.undo || 'Changes undone',
      };

    case 'SET_CUSTOM_MENU':
      return { ...state, customMenu: action.payload, isDirty: true };

    case 'CLEAR_MESSAGE':
      return { ...state, error: null, success: null };

    case 'SET_SHOW_GROUP_INPUT':
      return { ...state, showGroupInput: action.payload };

    case 'SET_NEW_GROUP_NAME':
      return { ...state, newGroupName: action.payload };

    case 'SET_EDITING_GROUP':
      return { ...state, editingGroupId: action.payload, newGroupName: action.payload ? state.customMenu.find(i => getItemId(i) === action.payload)?.title || '' : '' };

    case 'SET_ERROR':
      return { ...state, error: action.payload };

    case 'SET_SUCCESS':
      return { ...state, success: action.payload };

    default:
      return state;
  }
}
