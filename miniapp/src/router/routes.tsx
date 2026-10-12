import { createBrowserRouter, type RouteObject } from 'react-router-dom';
import { LoginGate } from '../features/auth/LoginGate';
import { AppShell } from '../components/layout/AppShell';
import { DashboardPage } from '../features/dashboard/DashboardPage';
import { BooksPage } from '../features/books/BooksPage';
import { BookDetailPage } from '../features/books/BookDetailPage';
import { AssignmentsPage } from '../features/assignments/AssignmentsPage';
import { AssignmentDetailPage } from '../features/assignments/AssignmentDetailPage';
import { AssistantPage } from '../features/assistant/AssistantPage';
import { SettingsPage } from '../features/settings/SettingsPage';
import { SubscriptionPage } from '../features/subscription/SubscriptionPage';
import { ClassesPage } from '../features/classes/ClassesPage';
import { ClassDetailPage } from '../features/classes/ClassDetailPage';
import { ClassStudentsPage } from '../features/classes/ClassStudentsPage';
import { StudentsPage } from '../features/students/StudentsPage';
import { NewStudentPage } from '../features/students/NewStudentPage';
import { StudentDetailPage } from '../features/students/StudentDetailPage';
import { ExamsPage } from '../features/exams/ExamsPage';
import { ExamGeneratePage } from '../features/exams/ExamGeneratePage';
import { ExamManagePage } from '../features/exams/ExamManagePage';
import { ExamTakePage } from '../features/exams/ExamTakePage';
import { ExamTeacherResultsPage } from '../features/exams/ExamTeacherResultsPage';
import { ExamAttemptResultsPage } from '../features/exams/ExamResultsPage';
import { GroupsPage } from '../features/groups/GroupsPage';
import { ChannelsPage } from '../features/channels/ChannelsPage';
import { AutomationRulesPage } from '../features/automation/AutomationRulesPage';
import { SchedulePage } from '../features/schedule/SchedulePage';
import { NotFoundPage } from '../features/misc/NotFoundPage';

/**
 * مسیرهای برنامه — {@code docs/mini-app.md} بخش ۶.
 *
 * <p>فاز ۳: کلاس‌ها و دانش‌آموزان با داده واقعی. فاز ۶: کتاب‌ها.
 * فاز ۷ گام ۱: تکالیف (فهرست/ساخت/جزئیات/ارسال پاسخ).
 * فاز ۸: برگزاری و نتایج آزمون. فاز ۹: گروه‌ها، کانال‌ها، قواعد، زمان‌بندی.
 */
export const routes: RouteObject[] = [
  {
    path: '/',
    element: (
      <LoginGate>
        <AppShell />
      </LoginGate>
    ),
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'books', element: <BooksPage /> },
      { path: 'books/:id', element: <BookDetailPage /> },
      { path: 'assignments', element: <AssignmentsPage /> },
      { path: 'assignments/:id', element: <AssignmentDetailPage /> },
      { path: 'assistant', element: <AssistantPage /> },
      { path: 'settings', element: <SettingsPage /> },
      { path: 'subscription', element: <SubscriptionPage /> },
      { path: 'classes', element: <ClassesPage /> },
      { path: 'classes/:id', element: <ClassDetailPage /> },
      { path: 'classes/:id/students', element: <ClassStudentsPage /> },
      { path: 'students', element: <StudentsPage /> },
      { path: 'students/new', element: <NewStudentPage /> },
      { path: 'students/:id', element: <StudentDetailPage /> },
      { path: 'exams', element: <ExamsPage /> },
      { path: 'exams/generate', element: <ExamGeneratePage /> },
      { path: 'exams/:id/manage', element: <ExamManagePage /> },
      { path: 'exams/:id/take', element: <ExamTakePage /> },
      { path: 'exams/:id/results', element: <ExamTeacherResultsPage /> },
      { path: 'exams/:id', element: <ExamTakePage /> },
      { path: 'exams/attempts/:attemptId/results', element: <ExamAttemptResultsPage /> },
      { path: 'groups', element: <GroupsPage /> },
      { path: 'channels', element: <ChannelsPage /> },
      { path: 'automation-rules', element: <AutomationRulesPage /> },
      { path: 'schedule', element: <SchedulePage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];

/**
 * ساخت روتر مرورگر.
 *
 * <p>⚠️ تابع است و نه نمونه ثابت، تا آزمون‌ها بتوانند روتر حافظه‌ای خودشان
 * را از همین {@link routes} بسازند بدون آنکه به {@code window.history}
 * وابسته باشند.
 */
export function createAppRouter() {
  return createBrowserRouter(routes, {
    basename: import.meta.env.BASE_URL,
  });
}
