/** کاربر — دقیقاً `AuthUserView` در بک‌اند. */
export interface AuthUser {
  id: string;
  fullName: string;
  roles: string[];
}

/** مستأجر فعال — دقیقاً `AuthTenantView`. */
export interface AuthTenant {
  id: string;
  name: string;
  type: string;
}

/**
 * اشتراک — در فاز ۲ همیشه `null`.
 *
 * <p>⚠️ به‌صورت `unknown` نگه داشته می‌شود تا وقتی فاز ۱۰ شکل واقعی‌اش را
 * داد، اینجا یک نوع خیالی وجود نداشته باشد. نوع خیالی از نبودِ نوع
 * بدتر است: کد کامپایل می‌شود و روی فیلدی کار می‌کند که هرگز نمی‌آید.
 *
 * <p>⚠️ `unknown` خودش `null` را هم در بر می‌گیرد، پس نوشتن
 * `unknown | null` بی‌معنی است و فقط این توهم را می‌سازد که دو حالت
 * جدا مدل شده‌اند.
 */
export type AuthSubscription = unknown;

/** پاسخ ورود/تازه‌سازی — دقیقاً `AuthResponse` در بک‌اند. */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  refreshExpiresIn: number;
  user: AuthUser;
  tenant: AuthTenant | null;
  subscription: AuthSubscription;
  isNewUser: boolean;
}

/** پاسخ `GET /auth/me` — دقیقاً `CurrentUserResponse`. */
export interface CurrentUserResponse {
  user: AuthUser;
  tenant: AuthTenant | null;
  subscription: AuthSubscription;
}

/**
 * شکل خطای بک‌اند — دقیقاً `ApiError` در بک‌اند.
 *
 * <p>⚠️ فیلدهای اختیاری واقعاً اختیاری‌اند: بک‌اند با
 * `@JsonInclude(NON_NULL)` سریالایز می‌کند، پس `details` یا حتی `traceId`
 * ممکن است در پاسخ نباشند. اگر اینجا اجباری تعریفشان می‌کردیم، کد روی
 * فیلدی تکیه می‌کرد که گاهی `undefined` است.
 *
 * <p>🔴 <b>چرا {@code code} روی سیم {@code string} است و نه
 * {@code ApiErrorCode}؟</b> چون بک‌اند ممکن است کدی را <i>پیش از</i> به‌روز
 * شدن این آینه اضافه کند. اگر اینجا فقط ۲۵ کد شناخته‌شده را می‌پذیرفتیم،
 * پاسخ واقعی سرور یک خطای <i>نوع‌گذاری</i> می‌ساخت — در حالی که داده سالم
 * است و فقط ما آن را نمی‌شناسیم. آن‌وقت کاربر به‌جای پیام خطای درست، یک
 * خطای کلاینت می‌دید.
 *
 * <p>هرجا لازم بود به فهرست شناخته‌شده محدود شوید، از
 * {@code isKnownErrorCode} استفاده کنید.
 */
export interface ApiErrorBody {
  timestamp?: string;
  status?: number;
  code?: string;
  message?: string;
  traceId?: string;
  path?: string;
  details?: unknown;
}

/**
 * صفحه استاندارد بک‌اند — دقیقاً `PageResponse`.
 *
 * <p>⚠️ شکل Spring Data خام نیست؛ بک‌اند خودش پوشش می‌دهد تا قرارداد
 * API به نسخه Spring وابسته نشود.
 */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

/** کلاس — دقیقاً `ClassRoomResponse`. */
export interface ClassRoom {
  id: string;
  name: string;
  gradeLevel: string | null;
  academicYear: string | null;
  subjectId: string | null;
  subjectName: string | null;
  homeroomTeacherId: string | null;
  capacity: number | null;
  status: 'ACTIVE' | 'ARCHIVED';
  createdAt: string;
  updatedAt: string;
}

/** داشبورد کلاس — دقیقاً `ClassDashboardResponse`. */
export interface ClassDashboard {
  classId: string;
  name: string;
  gradeLevel: string | null;
  academicYear: string | null;
  subjectId: string | null;
  subjectName: string | null;
  status: 'ACTIVE' | 'ARCHIVED';
  capacity: number | null;
  enrolledStudentCount: number;
  seatsRemaining: number | null;
  createdAt: string;
  updatedAt: string;
}

/** دانش‌آموز ثبت‌نام‌شده در کلاس — دقیقاً `ClassStudentResponse`. */
export interface ClassStudent {
  studentId: string;
  fullName: string;
  nationalCode: string | null;
  rollNumber: string | null;
  enrolledAt: string;
}

/** دانش‌آموز — دقیقاً `StudentResponse`. */
export interface Student {
  id: string;
  fullName: string;
  nationalCode: string | null;
  guardianName: string | null;
  guardianMobile: string | null;
  birthDate: string | null;
  gender: 'MALE' | 'FEMALE' | null;
  status: 'ACTIVE' | 'INACTIVE' | 'GRADUATED';
  createdAt: string;
  updatedAt: string;
}

/** ورودی ساخت کلاس. */
export interface CreateClassRoomBody {
  name: string;
  gradeLevel?: string | null;
  academicYear?: string | null;
  subjectId?: string | null;
  capacity?: number | null;
}

/** ورودی ساخت دانش‌آموز. */
export interface CreateStudentBody {
  fullName: string;
  nationalCode?: string | null;
  guardianName?: string | null;
  guardianMobile?: string | null;
  birthDate?: string | null;
  gender?: 'MALE' | 'FEMALE' | null;
}

/** ورودی ثبت‌نام در کلاس. */
export interface EnrollStudentBody {
  studentId: string;
  rollNumber?: string | null;
}

/** کتاب — دقیقاً `BookResponse`. */
export interface Book {
  id: string;
  title: string;
  publisher: string | null;
  gradeLevel: string | null;
  subjectId: string | null;
  totalPages: number | null;
  status: 'DRAFT' | 'PROCESSING' | 'READY' | 'FAILED';
  coverFileId: string | null;
  createdAt: string;
  updatedAt: string;
}

/** صفحه کتاب — دقیقاً `BookPageResponse`. */
export interface BookPage {
  id: string;
  bookId: string;
  pageNumber: number;
  fileId: string;
  extractedText: string | null;
  ocrStatus: 'PENDING' | 'DONE' | 'FAILED' | 'SKIPPED';
  ocrConfidence: number | null;
  createdAt: string;
  updatedAt: string;
}

/** تحلیل محتوا — `ContentAnalysisResponse`. */
export interface ContentAnalysis {
  id: string;
  targetType: 'BOOK' | 'PAGE' | 'CHAPTER';
  targetId: string;
  analysisType: 'SUMMARY' | 'QUESTIONS' | 'TEACHING_PLAN' | 'DIFFICULTY';
  resultText: string | null;
  createdAt: string;
}

/** ورودی ساخت کتاب. */
export interface CreateBookBody {
  title: string;
  publisher?: string | null;
  gradeLevel?: string | null;
  subjectId?: string | null;
}

/** تکلیف — دقیقاً `AssignmentResponse`. */
export interface Assignment {
  id: string;
  classroomId: string;
  classroomName: string | null;
  bookId: string | null;
  pageFrom: number | null;
  pageTo: number | null;
  title: string;
  description: string | null;
  type: 'HOMEWORK' | 'PROJECT' | 'PRACTICE' | 'QUIZ';
  maxScore: number | null;
  dueAt: string | null;
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'ARCHIVED';
  aiEvaluationEnabled: boolean;
  submissionCount: number;
  eitaaChatId: string | null;
  eitaaMessageStatus: 'SENT' | 'FAILED' | 'SKIPPED' | null;
  createdAt: string;
  updatedAt: string;
}

/** ارسال تکلیف — دقیقاً `SubmissionResponse`. */
export interface AssignmentSubmission {
  id: string;
  assignmentId: string;
  studentId: string;
  studentName: string | null;
  answerText: string | null;
  status: string;
  submittedAt: string;
  submittedByUserId: string | null;
  finalScore: number | null;
  teacherOverrodeAi: boolean;
}

/** ارزیابی AI — `EvaluationResponse`. */
export interface Evaluation {
  id: string;
  submissionId: string;
  status: string;
  aiScore: number | null;
  correct: boolean | null;
  mistakes: string[];
  strengths: string[];
  weaknesses: string[];
  feedback: string | null;
  teacherNote: string | null;
  confidence: number | null;
  requiresTeacherReview: boolean;
  finalScore: number | null;
}

export type ReviewAction =
  | 'ACCEPT'
  | 'EDIT_SCORE'
  | 'EDIT_FEEDBACK'
  | 'REJECT'
  | 'RE_EVALUATE';

/** بازبینی معلم — `TeacherReviewResponse`. */
export interface TeacherReview {
  id: string;
  submissionId: string;
  reviewerUserId: string;
  action: ReviewAction;
  previousScore: number | null;
  newScore: number | null;
  comment: string | null;
  reviewedAt: string;
}

/** ورودی ساخت تکلیف. */
export interface CreateAssignmentBody {
  classroomId: string;
  title: string;
  description?: string | null;
  type?: 'HOMEWORK' | 'PROJECT' | 'PRACTICE' | 'QUIZ';
  maxScore?: number | null;
  dueAt?: string | null;
  bookId?: string | null;
  pageFrom?: number | null;
  pageTo?: number | null;
  aiEvaluationEnabled?: boolean;
}

/** ورودی ثبت پاسخ. */
export interface CreateSubmissionBody {
  studentId: string;
  answerText: string;
}

/** ورودی انتشار. */
export interface PublishAssignmentBody {
  eitaaChatId?: string | null;
}

/** پاسخ دستیار — `AiChatResponse`. */
export interface AiChatResponse {
  content: string;
  model: string;
  provider: string;
  promptTokens: number;
  completionTokens: number;
  cost: number | string;
  usageRecordId: string;
}

/** ورودی گفت‌وگو با دستیار. */
export interface AiChatBody {
  message: string;
  model?: string;
}

