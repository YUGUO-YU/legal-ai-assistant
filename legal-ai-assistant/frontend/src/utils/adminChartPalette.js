export const adminChartPalette = [
  '#6366f1',
  '#10b981',
  '#f59e0b',
  '#ef4444',
  '#06b6d4',
  '#8b5cf6',
  '#ec4899',
  '#84cc16',
  '#f97316',
];

export const chartBorderColor = '#ffffff';

export const adminConfidenceColors = {
  high: '#10b981',
  medium: '#f59e0b',
  low: '#ef4444',
};

export function getConfidenceColor(value) {
  if (value >= 0.7) return adminConfidenceColors.high;
  if (value >= 0.4) return adminConfidenceColors.medium;
  return adminConfidenceColors.low;
}
