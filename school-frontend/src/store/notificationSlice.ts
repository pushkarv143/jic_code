import { createSlice, type PayloadAction } from '@reduxjs/toolkit';

/**
 * In-app notification bell contents. Round 1 seeds a small realistic sample so
 * the TopBar notification menu isn't empty; later rounds will replace this
 * with data fetched from GET /api/v1/notifications.
 */
export interface AppNotification {
  id: number;
  title: string;
  message: string;
  createdAt: string;
  read: boolean;
  type: 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR';
}

export interface NotificationState {
  items: AppNotification[];
}

const initialState: NotificationState = {
  items: [
    {
      id: 1,
      title: 'Fee due reminder',
      message: 'Term 2 fees are due on 15 Aug 2026 for Class 8-A.',
      createdAt: new Date().toISOString(),
      read: false,
      type: 'WARNING',
    },
    {
      id: 2,
      title: 'New circular published',
      message: 'Independence Day celebration schedule has been published.',
      createdAt: new Date().toISOString(),
      read: false,
      type: 'INFO',
    },
    {
      id: 3,
      title: 'Timetable updated',
      message: 'Class 6-B timetable was updated for the new term.',
      createdAt: new Date().toISOString(),
      read: true,
      type: 'SUCCESS',
    },
  ],
};

const notificationSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    setNotifications: (state, action: PayloadAction<AppNotification[]>) => {
      state.items = action.payload;
    },
    addNotification: (state, action: PayloadAction<AppNotification>) => {
      state.items.unshift(action.payload);
    },
    markAsRead: (state, action: PayloadAction<number>) => {
      const item = state.items.find((n) => n.id === action.payload);
      if (item) item.read = true;
    },
    markAllAsRead: (state) => {
      state.items.forEach((n) => {
        n.read = true;
      });
    },
    clearNotifications: (state) => {
      state.items = [];
    },
  },
});

export const {
  setNotifications,
  addNotification,
  markAsRead,
  markAllAsRead,
  clearNotifications,
} = notificationSlice.actions;
export default notificationSlice.reducer;
