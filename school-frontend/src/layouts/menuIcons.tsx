import type { ReactNode } from 'react';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import ClassOutlinedIcon from '@mui/icons-material/ClassOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import EventBusyOutlinedIcon from '@mui/icons-material/EventBusyOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import DirectionsBusOutlinedIcon from '@mui/icons-material/DirectionsBusOutlined';
import ApartmentOutlinedIcon from '@mui/icons-material/ApartmentOutlined';
import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined';
import RequestQuoteOutlinedIcon from '@mui/icons-material/RequestQuoteOutlined';
import BarChartOutlinedIcon from '@mui/icons-material/BarChartOutlined';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import ManageAccountsOutlinedIcon from '@mui/icons-material/ManageAccountsOutlined';
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined';
import LibraryBooksOutlinedIcon from '@mui/icons-material/LibraryBooksOutlined';
import VideoCameraFrontOutlinedIcon from '@mui/icons-material/VideoCameraFrontOutlined';
import CampaignOutlinedIcon from '@mui/icons-material/CampaignOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import HowToRegOutlinedIcon from '@mui/icons-material/HowToRegOutlined';
import FamilyRestroomOutlinedIcon from '@mui/icons-material/FamilyRestroomOutlined';
import NotificationsOutlinedIcon from '@mui/icons-material/NotificationsOutlined';
import ChatOutlinedIcon from '@mui/icons-material/ChatOutlined';
import AdminPanelSettingsOutlinedIcon from '@mui/icons-material/AdminPanelSettingsOutlined';
import CircleOutlinedIcon from '@mui/icons-material/CircleOutlined';

/**
 * The icons a seeded menu can name.
 *
 * Explicit imports rather than a dynamic `@mui/icons-material/${name}`: a
 * template import defeats tree-shaking and pulls the entire icon set — several MB
 * — into the bundle.
 */
const ICONS: Record<string, ReactNode> = {
  DashboardOutlined: <DashboardOutlinedIcon />,
  SchoolOutlined: <SchoolOutlinedIcon />,
  GroupsOutlined: <GroupsOutlinedIcon />,
  BadgeOutlined: <BadgeOutlinedIcon />,
  ClassOutlined: <ClassOutlinedIcon />,
  EventAvailableOutlined: <EventAvailableOutlinedIcon />,
  EventBusyOutlined: <EventBusyOutlinedIcon />,
  PaidOutlined: <PaidOutlinedIcon />,
  MenuBookOutlined: <MenuBookOutlinedIcon />,
  DirectionsBusOutlined: <DirectionsBusOutlinedIcon />,
  ApartmentOutlined: <ApartmentOutlinedIcon />,
  AssignmentOutlined: <AssignmentOutlinedIcon />,
  RequestQuoteOutlined: <RequestQuoteOutlinedIcon />,
  BarChartOutlined: <BarChartOutlinedIcon />,
  SettingsOutlined: <SettingsOutlinedIcon />,
  ManageAccountsOutlined: <ManageAccountsOutlinedIcon />,
  PersonOutlineOutlined: <PersonOutlineOutlinedIcon />,
  FactCheckOutlined: <FactCheckOutlinedIcon />,
  LibraryBooksOutlined: <LibraryBooksOutlinedIcon />,
  VideoCameraFrontOutlined: <VideoCameraFrontOutlinedIcon />,
  CampaignOutlined: <CampaignOutlinedIcon />,
  CalendarMonthOutlined: <CalendarMonthOutlinedIcon />,
  HowToRegOutlined: <HowToRegOutlinedIcon />,
  FamilyRestroomOutlined: <FamilyRestroomOutlinedIcon />,
  NotificationsOutlined: <NotificationsOutlinedIcon />,
  ChatOutlined: <ChatOutlinedIcon />,
  AdminPanelSettingsOutlined: <AdminPanelSettingsOutlinedIcon />,
};

/**
 * Renders the icon a menu row names.
 *
 * <p>`menus.icon` stores a name ('DashboardOutlined'), not an asset, because the
 * same row feeds the web app and the Android app and neither can use the other's
 * icon format. The cost is the map above; the benefit is that adding a menu is a
 * row rather than a release of two clients.
 *
 * <p>An unrecognised or missing name falls back to a neutral bullet rather than
 * rendering nothing — a menu whose icon someone mistyped should still be
 * clickable, and an empty icon slot collapses the row it sits in.
 *
 * <p>A component rather than a `menuIcon(name)` helper so this module exports
 * components only: a mixed module breaks Vite's fast refresh for every file that
 * imports it.
 */
export function MenuIcon({ name }: { name: string | null | undefined }) {
  return <>{(name && ICONS[name]) || <CircleOutlinedIcon />}</>;
}
