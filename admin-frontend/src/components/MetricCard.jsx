import { formatNumber } from '../utils/format.js';

export function MetricCard({ label, value, suffix, icon }) {
  return (
    <div className="metricCard">
      <div className="metricIcon">{icon}</div>
      <span>{label}</span>
      <strong>{formatNumber(value)}{suffix ? ` ${suffix}` : ''}</strong>
    </div>
  );
}
