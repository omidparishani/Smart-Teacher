import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { PageHeader } from '../../components/shared/PageHeader';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import { sendChatMessage } from '../../lib/ai-api';

interface ChatTurn {
  id: string;
  role: 'user' | 'assistant';
  text: string;
}

/**
 * دستیار معلم — گفت‌وگوی واقعی با {@code POST /ai/chat} (مسیر Mock).
 */
export function AssistantPage() {
  const [message, setMessage] = useState('');
  const [turns, setTurns] = useState<ChatTurn[]>([]);
  const [error, setError] = useState<string | null>(null);

  const chatMutation = useMutation({
    mutationFn: (text: string) => sendChatMessage({ message: text }),
    onSuccess: (response, text) => {
      setTurns((prev) => [
        ...prev,
        { id: `u-${Date.now()}`, role: 'user', text },
        {
          id: response.usageRecordId,
          role: 'assistant',
          text: response.content,
        },
      ]);
      setMessage('');
      setError(null);
    },
    onError: (err) => {
      setError(err instanceof ApiError ? err.message : 'ارسال پیام ناموفق بود.');
    },
  });

  return (
    <>
      <PageHeader title="دستیار" description="پرسش از دستیار هوشمند" />

      <div className="mb-4 flex min-h-[12rem] flex-col gap-3 rounded-xl border border-border bg-card p-4">
        {turns.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            سؤالی بپرسید؛ پاسخ از سرویس هوش مصنوعی (Mock در محیط توسعه) می‌آید.
          </p>
        ) : (
          turns.map((turn) => (
            <div
              key={turn.id}
              className={
                turn.role === 'user'
                  ? 'self-start rounded-lg bg-primary/10 px-3 py-2 text-sm'
                  : 'self-stretch rounded-lg bg-muted/40 px-3 py-2 text-sm'
              }
            >
              <p className="mb-1 text-xs font-medium text-muted-foreground">
                {turn.role === 'user' ? 'شما' : 'دستیار'}
              </p>
              <p className="whitespace-pre-wrap">{turn.text}</p>
            </div>
          ))
        )}
      </div>

      {error ? (
        <p className="mb-3 text-sm text-destructive" role="alert">
          {error}
        </p>
      ) : null}

      <form
        className="flex flex-col gap-3"
        onSubmit={(event) => {
          event.preventDefault();
          const text = message.trim();
          if (!text || chatMutation.isPending) {
            return;
          }
          chatMutation.mutate(text);
        }}
      >
        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium">پیام</span>
          <textarea
            value={message}
            onChange={(event) => setMessage(event.target.value)}
            rows={3}
            maxLength={4000}
            className="rounded-xl border border-border bg-background px-3 py-2"
            placeholder="مثلاً یک ایده برای شروع درس کسر…"
          />
        </label>
        <Button type="submit" disabled={chatMutation.isPending || message.trim().length === 0}>
          {chatMutation.isPending ? 'در حال ارسال…' : 'ارسال'}
        </Button>
      </form>
    </>
  );
}
