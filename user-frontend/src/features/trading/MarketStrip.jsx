import { Radio } from 'lucide-react';
import { formatNumber } from '../../utils/format.js';

export function MarketStrip({ klines, price, trades, updatedAt }) {
  const first = Number(klines[0]?.open ?? price?.price ?? 0);
  const current = Number(price?.price ?? klines.at(-1)?.close ?? 0);
  const change = first ? ((current - first) / first) * 100 : 0;
  const quantities = trades.reduce((sum, trade) => sum + Number(trade.quantity || 0), 0);
  const high = klines.length ? Math.max(...klines.map((item) => Number(item.high))) : current;
  const low = klines.length ? Math.min(...klines.map((item) => Number(item.low))) : current;
  const positive = change >= 0;

  return (
    <section className="marketStrip" id="terminal">
      <div className="pairIdentity">
        <span className="coinMark">{price?.baseAsset?.slice(0, 1) || 'B'}</span>
        <div><strong>{price?.symbol || 'BTC-USDT'}</strong><span>Spot</span></div>
      </div>
      <div className={`headlinePrice ${positive ? 'positive' : 'negative'}`}>
        <strong>{formatNumber(current)}</strong>
        <span>{positive ? '+' : ''}{change.toFixed(2)}%</span>
      </div>
      <MarketMetric label="Mark price" value={formatNumber(current)} />
      <MarketMetric label="Session low" value={formatNumber(low)} />
      <MarketMetric label="Session high" value={formatNumber(high)} />
      <MarketMetric label={`Volume (${price?.baseAsset || 'BASE'})`} value={formatNumber(quantities)} />
      <div className="liveState"><Radio size={13} /> LIVE <span>{updatedAt ? new Date(updatedAt).toLocaleTimeString() : '--:--:--'}</span></div>
    </section>
  );
}

function MarketMetric({ label, value }) {
  return <div className="marketMetric"><span>{label}</span><strong>{value}</strong></div>;
}
