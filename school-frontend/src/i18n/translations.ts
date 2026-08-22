/**
 * Minimal translation dictionary powering useTranslation() (see LanguageProvider.tsx).
 * This is intentionally a plain nested-object lookup rather than a full i18n
 * library — enough to prove the mechanism end-to-end (persisted language
 * selection -> real UI text change) without adding a new dependency.
 *
 * Coverage so far: sidebar navigation labels + the admin dashboard's stat card
 * labels. Extending coverage to every page's copy is future work — add more
 * keys here and read them via useTranslation() as each page is migrated.
 */
export type Language = 'en' | 'hi';

export const LANGUAGE_LABELS: Record<Language, string> = {
  en: 'English',
  hi: 'हिन्दी (Hindi)',
};

export const translations = {
  en: {
    nav: {
      dashboard: 'Dashboard',
      myChildren: 'My Children',
      students: 'Students',
      teachers: 'Teachers',
      myClass: 'My Class',
      classesSubjects: 'Classes & Subjects',
      myTimetable: 'My Timetable',
      attendance: 'Attendance',
      leave: 'Leave',
      examsMarks: 'Exams & Marks',
      assignments: 'Assignments',
      studyMaterials: 'Study Materials',
      onlineClasses: 'Online Classes',
      staff: 'Staff',
      fees: 'Fees',
      payroll: 'Payroll',
      library: 'Library',
      transport: 'Transport',
      hostel: 'Hostel',
      admissionEnquiries: 'Admission Enquiries',
      noticeBoard: 'Notice Board',
      calendar: 'Calendar',
      notifications: 'Notifications',
      chat: 'Chat',
      reports: 'Reports',
      myProfile: 'My Profile',
      settings: 'Settings',
    },
    dashboard: {
      activeStudents: 'Active Students',
      activeTeachers: 'Active Teachers',
      averageAttendance: 'Average Attendance',
      feesCollected: 'Fees Collected',
      feesOutstanding: 'Fees Outstanding',
      payrollPending: 'Payroll Pending (This Month)',
    },
  },
  hi: {
    nav: {
      dashboard: 'डैशबोर्ड',
      myChildren: 'मेरे बच्चे',
      students: 'छात्र',
      teachers: 'शिक्षक',
      myClass: 'मेरी कक्षा',
      classesSubjects: 'कक्षाएँ और विषय',
      myTimetable: 'मेरी समय-सारणी',
      attendance: 'उपस्थिति',
      leave: 'अवकाश',
      examsMarks: 'परीक्षा और अंक',
      assignments: 'असाइनमेंट',
      studyMaterials: 'अध्ययन सामग्री',
      onlineClasses: 'ऑनलाइन कक्षाएँ',
      staff: 'कर्मचारी',
      fees: 'शुल्क',
      payroll: 'वेतन',
      library: 'पुस्तकालय',
      transport: 'परिवहन',
      hostel: 'छात्रावास',
      admissionEnquiries: 'प्रवेश पूछताछ',
      noticeBoard: 'सूचना पट्ट',
      calendar: 'कैलेंडर',
      notifications: 'सूचनाएँ',
      chat: 'चैट',
      reports: 'रिपोर्ट',
      myProfile: 'मेरी प्रोफ़ाइल',
      settings: 'सेटिंग्स',
    },
    dashboard: {
      activeStudents: 'सक्रिय छात्र',
      activeTeachers: 'सक्रिय शिक्षक',
      averageAttendance: 'औसत उपस्थिति',
      feesCollected: 'एकत्रित शुल्क',
      feesOutstanding: 'शेष शुल्क',
      payrollPending: 'लंबित वेतन (इस माह)',
    },
  },
} as const satisfies Record<Language, Record<string, Record<string, string>>>;

export default translations;
