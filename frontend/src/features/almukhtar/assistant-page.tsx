'use client';

import { Icons } from '@/components/icons';
import PageContainer from '@/components/layout/page-container';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { apiClient } from '@/lib/api-client';
import { useEffect, useRef, useState } from 'react';
import { toast } from 'sonner';
import { useSession } from './session';
import type { AiChatMessage, AiChatResponse } from './types';

const QUICK_PROMPTS = [
  'ما هو رصيد الصناديق التشغيلية الحالي؟',
  'كم عدد الحوالات المعلقة اليوم؟',
  'هل توجد تنبيهات سيولة مفتوحة؟',
  'ما هي أسعار الصرف الحالية لل USD؟',
  'كم عدد تنبيهات AML المفتوحة؟',
  'ما هي الحوالات التي تحتاج مراجعة؟',
];

function MessageBubble({ msg }: { msg: AiChatMessage }) {
  const isUser = msg.role === 'user';
  return (
    <div className={`flex gap-2.5 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
      {/* Avatar */}
      <div
        className={`shrink-0 size-7 rounded-full flex items-center justify-center text-xs font-bold mt-0.5 ${
          isUser
            ? 'bg-foreground text-background'
            : 'bg-emerald-600/20 text-emerald-700 dark:text-emerald-400 border border-emerald-600/30'
        }`}
      >
        {isUser ? 'أنت' : 'AI'}
      </div>

      {/* Bubble */}
      <div
        className={`max-w-[75%] rounded-md px-3 py-2 text-sm leading-relaxed ${
          isUser
            ? 'bg-foreground text-background rounded-tl-none'
            : 'gov-panel border border-border rounded-tr-none'
        }`}
      >
        <div className='whitespace-pre-wrap'>{msg.content}</div>
        <div
          className={`text-xs mt-1.5 ${
            isUser ? 'text-background/60' : 'text-muted-foreground'
          }`}
        >
          {msg.timestamp.toLocaleTimeString('ar-SY', { hour: '2-digit', minute: '2-digit' })}
        </div>
      </div>
    </div>
  );
}

function TypingIndicator() {
  return (
    <div className='flex gap-2.5 flex-row'>
      <div className='shrink-0 size-7 rounded-full flex items-center justify-center text-xs font-bold bg-emerald-600/20 text-emerald-700 dark:text-emerald-400 border border-emerald-600/30'>
        AI
      </div>
      <div className='gov-panel border border-border rounded-md rounded-tr-none px-3 py-2.5'>
        <div className='flex items-center gap-1'>
          <span className='size-1.5 rounded-full bg-muted-foreground/60 animate-bounce [animation-delay:0ms]' />
          <span className='size-1.5 rounded-full bg-muted-foreground/60 animate-bounce [animation-delay:150ms]' />
          <span className='size-1.5 rounded-full bg-muted-foreground/60 animate-bounce [animation-delay:300ms]' />
        </div>
      </div>
    </div>
  );
}

export function AssistantPage() {
  const { session } = useSession();
  const [messages, setMessages] = useState<AiChatMessage[]>([
    {
      id: 'welcome',
      role: 'assistant',
      content: `مرحباً ${session?.username ?? 'بك'}! أنا مساعد العمليات الذكي لمكتب المختار.\n\nيمكنني مساعدتك في:\n• الاستفسار عن أرصدة الصناديق والمحافظ\n• مراجعة حالة الحوالات والتنبيهات\n• الإجابة عن أسئلة العمليات التشغيلية\n• تحليل بيانات السيولة والامتثال\n\nكيف يمكنني مساعدتك اليوم؟`,
      timestamp: new Date()
    }
  ]);
  const [input, setInput] = useState('');
  const [thinking, setThinking] = useState(false);
  const [sessionId] = useState(() => `session-${Date.now()}`);
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, thinking]);

  async function sendMessage(text: string) {
    const userText = text.trim();
    if (!userText || thinking) return;

    const userMsg: AiChatMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      content: userText,
      timestamp: new Date()
    };

    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setThinking(true);

    try {
      // Try the AI helper endpoint first, then fallback to trading assistant
      let responseText: string | null = null;

      const endpoints = [
        { url: '/ai/helper/message', body: { message: userText, sessionId } },
        { url: '/ai/chat', body: { message: userText, sessionId } },
        { url: '/trading/assistant/chat', body: { message: userText } }
      ];

      for (const ep of endpoints) {
        try {
          const res = await apiClient<AiChatResponse>(ep.url, {
            method: 'POST',
            body: JSON.stringify(ep.body)
          });
          responseText =
            res?.response ??
            res?.message ??
            res?.answer ??
            res?.reply ??
            (typeof res === 'string' ? res : null);
          if (responseText) break;
        } catch {
          // Try next endpoint
        }
      }

      const assistantMsg: AiChatMessage = {
        id: crypto.randomUUID(),
        role: 'assistant',
        content: responseText ?? 'عذراً، لم أتمكن من الحصول على رد من الخادم. تأكد من أن الخادم يعمل وأنك مسجل الدخول.',
        timestamp: new Date()
      };

      setMessages((prev) => [...prev, assistantMsg]);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإرسال إلى المساعد');
      setMessages((prev) => [
        ...prev,
        {
          id: crypto.randomUUID(),
          role: 'assistant',
          content: 'تعذر الاتصال بالمساعد. يرجى التحقق من حالة الخادم والمحاولة مجدداً.',
          timestamp: new Date()
        }
      ]);
    } finally {
      setThinking(false);
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    void sendMessage(input);
  }

  function handleClear() {
    setMessages([
      {
        id: 'welcome-new',
        role: 'assistant',
        content: 'تم مسح المحادثة. كيف يمكنني مساعدتك؟',
        timestamp: new Date()
      }
    ]);
  }

  return (
    <PageContainer
      pageTitle='مساعد العمليات الذكي'
      pageDescription='محادثة مع مساعد الذكاء الاصطناعي للاستفسار عن بيانات العمليات وتحليل الأداء.'
      pageHeaderAction={
        <div className='flex items-center gap-2'>
          <Badge variant='outline' className='text-xs gap-1'>
            <span className='size-1.5 rounded-full bg-emerald-500 animate-pulse' />
            متصل
          </Badge>
          <Button
            variant='outline'
            size='sm'
            onClick={handleClear}
          >
            <Icons.trash className='me-1.5 size-4' />
            مسح المحادثة
          </Button>
        </div>
      }
    >
      <div className='grid gap-4 xl:grid-cols-[1fr_280px] h-[calc(100vh-220px)] min-h-[500px]'>
        {/* ── Chat Area ── */}
        <div className='gov-panel rounded-md flex flex-col overflow-hidden'>
          {/* Messages */}
          <div className='flex-1 overflow-y-auto p-4 space-y-4'>
            {messages.map((msg) => (
              <MessageBubble key={msg.id} msg={msg} />
            ))}
            {thinking && <TypingIndicator />}
            <div ref={bottomRef} />
          </div>

          {/* Input */}
          <div className='border-t border-border p-3'>
            <form onSubmit={handleSubmit} className='flex items-center gap-2'>
              <Input
                ref={inputRef}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder='اكتب سؤالك أو استفسارك عن العمليات...'
                className='flex-1 h-9 rounded-sm text-sm'
                disabled={thinking}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    void sendMessage(input);
                  }
                }}
              />
              <Button
                type='submit'
                size='sm'
                className='h-9 px-3 rounded-sm'
                disabled={thinking || !input.trim()}
              >
                {thinking ? (
                  <Icons.spinner className='size-4 animate-spin' />
                ) : (
                  <Icons.send className='size-4' />
                )}
              </Button>
            </form>
            <div className='text-xs text-muted-foreground mt-1.5 text-start'>
              اضغط Enter للإرسال • الردود من AI قد تحتاج تحقق بشري
            </div>
          </div>
        </div>

        {/* ── Quick Prompts Sidebar ── */}
        <div className='space-y-3'>
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-3 py-2.5'>
              <h3 className='text-xs font-semibold'>اقتراحات سريعة</h3>
            </div>
            <div className='p-2 space-y-1'>
              {QUICK_PROMPTS.map((prompt) => (
                <button
                  key={prompt}
                  type='button'
                  onClick={() => { void sendMessage(prompt); }}
                  disabled={thinking}
                  className='w-full text-right text-xs px-3 py-2 rounded-sm hover:bg-muted/50 transition-colors border border-transparent hover:border-border disabled:opacity-50 disabled:cursor-not-allowed text-muted-foreground hover:text-foreground'
                >
                  {prompt}
                </button>
              ))}
            </div>
          </div>

          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-3 py-2.5'>
              <h3 className='text-xs font-semibold'>معلومات الجلسة</h3>
            </div>
            <div className='p-3 space-y-2 text-xs'>
              <div className='flex justify-between items-center'>
                <span className='text-muted-foreground'>المستخدم</span>
                <span className='font-medium'>{session?.username ?? '—'}</span>
              </div>
              <div className='flex justify-between items-center'>
                <span className='text-muted-foreground'>الصلاحية</span>
                <Badge variant='outline' className='text-xs'>{session?.role ?? '—'}</Badge>
              </div>
              <div className='flex justify-between items-center'>
                <span className='text-muted-foreground'>الرسائل</span>
                <span className='font-mono'>{messages.length}</span>
              </div>
              <div className='flex justify-between items-center'>
                <span className='text-muted-foreground'>معرف الجلسة</span>
                <span className='font-mono text-[10px] truncate max-w-[100px]'>{sessionId.slice(-8)}</span>
              </div>
            </div>
          </div>

          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-3 py-2.5'>
              <h3 className='text-xs font-semibold'>تنبيه مهم</h3>
            </div>
            <div className='p-3 text-xs text-muted-foreground leading-relaxed'>
              <p>
                ردود المساعد الذكي مبنية على البيانات المتاحة وقد لا تكون دقيقة بالكامل.
                يُوصى دائماً بالتحقق من المعلومات الحساسة مباشرةً من لوحة التحكم.
              </p>
            </div>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}
