export interface MenuItem {
  id?: string;
  key?: string;
  title: string;
  subMenu?: MenuItem[];
  url?: string;
  badge?: { counter: number };
}

export interface MenuCategory {
  id: string;
  title: string;
  subMenu?: MenuItem[];
}

export interface Translations {
  [key: string]: string;
}

export interface MenuCustomizerState {
  loading: boolean;
  mainMenuStructured: MenuCategory[];
  mainMenuFlat: MenuItem[];
  customMenu: MenuItem[];
  lastSavedMenu: MenuItem[];
  translations: Translations;
  activeId: string | null;
  draggedItem: MenuItem | null;
  error: string | null;
  success: string | null;
  toast: string | null;
  isDirty: boolean;
  editingGroupId: string | null;
  showGroupInput: boolean;
  newGroupName: string;
  search: string;
  collapsedGroups: Set<string>;
  collapsedCategories: Set<string>;
}

export type MenuAction =
  | { type: 'LOAD_START' }
  | { type: 'LOAD_SUCCESS'; payload: { mainMenuStructured: MenuCategory[]; mainMenuFlat: MenuItem[]; customMenu: MenuItem[]; translations: Translations } }
  | { type: 'LOAD_FAILURE'; payload: string }
  | { type: 'DRAG_START'; payload: { id: string; item: MenuItem } }
  | { type: 'DRAG_END' }
  | { type: 'REORDER'; payload: { container: string; oldIndex: number; newIndex: number } }
  | { type: 'ADD_FROM_TEMPLATE'; payload: { item: MenuItem; targetContainer: string; targetIndex: number } }
  | { type: 'MOVE_TO_CONTAINER'; payload: { item: MenuItem; sourceContainer: string; sourceIndex: number; targetContainer: string; targetIndex: number } }
  | { type: 'REMOVE_ITEM'; payload: { itemId: string; groupId?: string } }
  | { type: 'ADD_GROUP'; payload: { name: string } }
  | { type: 'RENAME_GROUP'; payload: { groupId: string; name: string } }
  | { type: 'SAVE_START' }
  | { type: 'SAVE_SUCCESS' }
  | { type: 'SAVE_FAILURE'; payload: string }
  | { type: 'UNDO' }
  | { type: 'SET_CUSTOM_MENU'; payload: MenuItem[] }
  | { type: 'CLEAR_MESSAGE' }
  | { type: 'SET_SHOW_GROUP_INPUT'; payload: boolean }
  | { type: 'SET_NEW_GROUP_NAME'; payload: string }
  | { type: 'SET_EDITING_GROUP'; payload: string | null }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'SET_SUCCESS'; payload: string | null }
  | { type: 'SET_TOAST'; payload: string | null }
  | { type: 'SET_SEARCH'; payload: string }
  | { type: 'TOGGLE_GROUP_COLLAPSE'; payload: string }
  | { type: 'TOGGLE_CATEGORY_COLLAPSE'; payload: string };

export interface DragData {
  type: 'item' | 'group';
  item: MenuItem;
  container: string; // 'favorites' | 'template' | groupId
}
