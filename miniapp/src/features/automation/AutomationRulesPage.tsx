import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Zap } from 'lucide-react';
import { PageHeader } from '../../components/shared/PageHeader';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { EmptyState } from '../../components/shared/EmptyState';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import { createAutomationRule, listAutomationRules } from '../../lib/eitaa-api';

const TRIGGER_OPTIONS = [
  'ASSIGNMENT_PUBLISHED',
  'SUBMISSION_EVALUATED',
  'EXAM_COMPLETED',
  'SCORE_BELOW_THRESHOLD',
  'SUBSCRIPTION_EXPIRING',
] as const;

const ACTION_OPTIONS = ['NOTIFY_TEACHER', 'NOTIFY_STUDENT', 'SEND_MESSAGE'] as const;

/**
 * صفحه قواعد خودکارسازی — فهرست و ساخت.
 */
export function AutomationRulesPage() {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [triggerType, setTriggerType] = useState<string>(TRIGGER_OPTIONS[0]);
  const [triggerConfig, setTriggerConfig] = useState('{"thresholdPercent":50}');
  const [actionType, setActionType] = useState<string>(ACTION_OPTIONS[0]);
  const [actionConfig, setActionConfig] = useState('');
  const [note, setNote] = useState<string | null>(null);

  const rulesQuery = useQuery({
    queryKey: ['automation-rules'],
    queryFn: () => listAutomationRules(),
  });

  const createMutation = useMutation({
    mutationFn: () => {
      const body: {
        name: string;
        triggerType: string;
        triggerConfig?: string;
        actionType: string;
        actionConfig?: string;
        enabled: boolean;
      } = {
        name: name.trim(),
        triggerType,
        actionType,
        enabled: true,
      };
      const tc = triggerConfig.trim();
      if (tc) {
        body.triggerConfig = tc;
      }
      const ac = actionConfig.trim();
      if (ac) {
        body.actionConfig = ac;
      }
      return createAutomationRule(body);
    },
    onSuccess: async () => {
      setName('');
      setNote('قاعده ثبت شد.');
      await queryClient.invalidateQueries({ queryKey: ['automation-rules'] });
    },
    onError: (error) => {
      setNote(error instanceof ApiError ? error.message : 'ثبت قاعده ناموفق بود.');
    },
  });

  return (
    <>
      <PageHeader title="قواعد خودکارسازی" description="تریگر از رویدادهای داخلی سیستم" />

      {rulesQuery.isPending ? <LoadingSkeleton lines={3} /> : null}

      {rulesQuery.isError ? (
        <ErrorCard
          title="قواعد بارگذاری نشد"
          message={
            rulesQuery.error instanceof ApiError
              ? rulesQuery.error.message
              : 'ارتباط برقرار نشد.'
          }
        />
      ) : null}

      {rulesQuery.isSuccess && rulesQuery.data.length === 0 ? (
        <EmptyState
          icon={<Zap className="size-8" aria-hidden="true" />}
          title="هنوز قاعده‌ای نیست"
          description="اولین قاعده خودکارسازی را بسازید."
        />
      ) : null}

      {rulesQuery.isSuccess && rulesQuery.data.length > 0 ? (
        <ul className="mb-6 space-y-2">
          {rulesQuery.data.map((rule) => (
            <li key={rule.id} className="rounded-xl border border-border px-3 py-3 text-sm">
              <div className="font-medium">{rule.name}</div>
              <div className="text-muted-foreground">
                {rule.triggerType} → {rule.actionType}
                {rule.enabled ? '' : ' (غیرفعال)'}
              </div>
              <div className="text-xs text-muted-foreground">
                اجرا: {rule.executionCount}
              </div>
            </li>
          ))}
        </ul>
      ) : null}

      <section className="space-y-3">
        <h2 className="text-sm font-medium">قاعده جدید</h2>
        <input
          className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          placeholder="نام قاعده"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <select
          className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          value={triggerType}
          onChange={(e) => setTriggerType(e.target.value)}
        >
          {TRIGGER_OPTIONS.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
        <input
          className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          placeholder='تنظیم تریگر (JSON، مثلاً {"thresholdPercent":50})'
          value={triggerConfig}
          onChange={(e) => setTriggerConfig(e.target.value)}
        />
        <select
          className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          value={actionType}
          onChange={(e) => setActionType(e.target.value)}
        >
          {ACTION_OPTIONS.map((a) => (
            <option key={a} value={a}>
              {a}
            </option>
          ))}
        </select>
        <input
          className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          placeholder='تنظیم اقدام (JSON، مثلاً {"chatId":"@group"})'
          value={actionConfig}
          onChange={(e) => setActionConfig(e.target.value)}
        />
        <Button
          disabled={!name.trim() || createMutation.isPending}
          onClick={() => createMutation.mutate()}
        >
          ثبت قاعده
        </Button>
      </section>

      {note ? <p className="mt-4 text-sm text-muted-foreground">{note}</p> : null}
    </>
  );
}
