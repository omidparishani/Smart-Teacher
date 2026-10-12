import type { ReactNode } from 'react';

export interface StatCardProps {
  title: string;
  value: string | number;
  description?: string;
  trend?: {
    value: string;
    positive: boolean;
  };
  icon?: ReactNode;
  color?: 'blue' | 'emerald' | 'violet' | 'amber' | 'rose';
}

export function StatCard({
  title,
  value,
  description,
  trend,
  icon,
  color = 'blue',
}: StatCardProps) {
  const colorStyles = {
    blue: 'bg-blue-50 text-blue-600 border-blue-100',
    emerald: 'bg-emerald-50 text-emerald-600 border-emerald-100',
    violet: 'bg-violet-50 text-violet-600 border-violet-100',
    amber: 'bg-amber-50 text-amber-600 border-amber-100',
    rose: 'bg-rose-50 text-rose-600 border-rose-100',
  };

  return (
    <div className="bg-white rounded-xl border border-slate-200/80 p-5 shadow-xs transition-all hover:shadow-md hover:border-slate-300">
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{title}</span>
        {icon && (
          <div className={`w-10 h-10 rounded-lg flex items-center justify-center border ${colorStyles[color]}`}>
            {icon}
          </div>
        )}
      </div>
      <div className="mt-3 flex items-baseline gap-2">
        <span className="text-2xl font-black text-slate-900 tracking-tight font-mono">{value}</span>
        {trend && (
          <span
            className={`text-xs font-semibold flex items-center gap-0.5 ${
              trend.positive ? 'text-emerald-600' : 'text-rose-600'
            }`}
          >
            <span>{trend.positive ? '↑' : '↓'}</span>
            <span>{trend.value}</span>
          </span>
        )}
      </div>
      {description && <p className="mt-1 text-xs text-slate-500">{description}</p>}
    </div>
  );
}
