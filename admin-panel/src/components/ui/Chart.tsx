export interface DataPoint {
  label: string;
  value: number;
}

export interface AreaChartProps {
  data: DataPoint[];
  height?: number;
  color?: string;
  fillColor?: string;
}

export function AreaChart({
  data,
  height = 180,
  color = '#2563eb',
  fillColor = '#3b82f6',
}: AreaChartProps) {
  if (!data || data.length === 0) {
    return <div className="h-44 flex items-center justify-center text-xs text-slate-400">داده‌ای یافت نشد</div>;
  }

  const values = data.map((d) => d.value);
  const max = Math.max(...values, 10);
  const min = Math.min(...values, 0);
  const range = max - min || 1;

  const width = 600;
  const padding = 20;
  const innerWidth = width - padding * 2;
  const innerHeight = height - padding * 2;

  const points = data.map((d, index) => {
    const x = padding + (index / (data.length - 1 || 1)) * innerWidth;
    const y = height - padding - ((d.value - min) / range) * innerHeight;
    return { x, y, ...d };
  });

  const pathD = points.reduce((acc, p, i) => `${acc} ${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`, '');
  const lastPoint = points[points.length - 1] ?? { x: padding, y: height - padding };
  const firstPoint = points[0] ?? { x: padding, y: height - padding };
  const areaD = `${pathD} L ${lastPoint.x} ${height - padding} L ${firstPoint.x} ${height - padding} Z`;

  return (
    <div className="w-full">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="w-full h-auto overflow-visible select-none"
      >
        <defs>
          <linearGradient id="areaGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={fillColor} stopOpacity="0.25" />
            <stop offset="100%" stopColor={fillColor} stopOpacity="0.0" />
          </linearGradient>
        </defs>

        {/* Horizontal grid lines */}
        {[0, 0.33, 0.66, 1].map((ratio) => {
          const y = height - padding - ratio * innerHeight;
          const val = Math.round(min + ratio * range);
          return (
            <g key={ratio}>
              <line
                x1={padding}
                y1={y}
                x2={width - padding}
                y2={y}
                stroke="#f1f5f9"
                strokeWidth="1"
              />
              <text
                x={width - padding + 8}
                y={y + 3}
                fill="#94a3b8"
                fontSize="10"
                fontFamily="monospace"
              >
                {val}
              </text>
            </g>
          );
        })}

        {/* Area */}
        <path d={areaD} fill="url(#areaGradient)" />

        {/* Line */}
        <path d={pathD} fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />

        {/* Points */}
        {points.map((p, i) => (
          <circle
            key={i}
            cx={p.x}
            cy={p.y}
            r="3.5"
            fill="#ffffff"
            stroke={color}
            strokeWidth="2"
            className="transition-all hover:r-5 cursor-pointer"
          >
            <title>{`${p.label}: ${p.value}`}</title>
          </circle>
        ))}
      </svg>
      {/* Labels */}
      <div className="flex justify-between px-2 mt-2 text-[10px] text-slate-400 font-mono">
        <span>{data[0]?.label}</span>
        {data.length > 2 && <span>{data[Math.floor(data.length / 2)]?.label}</span>}
        <span>{data[data.length - 1]?.label}</span>
      </div>
    </div>
  );
}

export function BarChart({
  data,
  height = 160,
  barColor = '#3b82f6',
}: {
  data: DataPoint[];
  height?: number;
  barColor?: string;
}) {
  if (!data || data.length === 0) {
    return <div className="h-40 flex items-center justify-center text-xs text-slate-400">داده‌ای یافت نشد</div>;
  }

  const values = data.map((d) => d.value);
  const max = Math.max(...values, 10);

  return (
    <div className="w-full flex items-end gap-2 pt-6 pb-2" style={{ height }}>
      {data.map((item, i) => {
        const pct = Math.max(8, (item.value / max) * 100);
        return (
          <div key={i} className="flex-1 flex flex-col items-center gap-1.5 group h-full justify-end">
            <span className="text-[10px] font-mono text-slate-400 opacity-0 group-hover:opacity-100 transition-opacity">
              {item.value}
            </span>
            <div
              style={{ height: `${pct}%`, backgroundColor: barColor }}
              className="w-full rounded-t-sm transition-all group-hover:brightness-110"
              title={`${item.label}: ${item.value}`}
            />
            <span className="text-[9px] font-mono text-slate-400 truncate max-w-[36px]">
              {item.label}
            </span>
          </div>
        );
      })}
    </div>
  );
}
