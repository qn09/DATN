export function formatNumber(value) {
  if (value === null || value === undefined) return '-';
  return Number(value).toLocaleString('en-US', { maximumFractionDigits: 8 });
}

export function formatTime(value) {
  if (!value) return '-';
  return new Date(value).toLocaleString();
}
