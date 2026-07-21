import { SlidersHorizontal } from 'lucide-react';
import { formatNumber } from '../../utils/format.js';

export function TerminalOrderBook({ connected, orderBook, symbol }) {
  const [base, quote] = symbol.split('-');
  const asks = [...(orderBook?.asks || [])].slice(0, 9).reverse();
  const bids = (orderBook?.bids || []).slice(0, 9);
  const midpoint = Number(asks.at(-1)?.price || bids[0]?.price || 0);
  const maxQuantity = Math.max(0, ...asks.concat(bids).map((row) => Number(row.quantity)));

  return (
    <section className="orderBookPanel">
      <header className="terminalPanelHeader">
        <div className="panelTabs"><button className="active">Order book</button><button>Trades</button></div>
        <span className={`bookSource ${connected ? 'connected' : ''}`}><i /> Binance <SlidersHorizontal size={15} /></span>
      </header>
      <div className="bookColumns"><span>Price ({quote})</span><span>Amount ({base})</span><span>Total</span></div>
      <div className="terminalBookRows">
        {asks.map((row) => <BookRow key={`ask-${row.price}`} maxQuantity={maxQuantity} row={row} tone="ask" />)}
        <div className="midPrice">{midpoint ? formatNumber(midpoint) : '--'} <span>{quote}</span></div>
        {bids.map((row) => <BookRow key={`bid-${row.price}`} maxQuantity={maxQuantity} row={row} tone="bid" />)}
        {!asks.length && !bids.length && <p className="terminalEmpty">Connecting to Binance depth...</p>}
      </div>
    </section>
  );
}

function BookRow({ maxQuantity, row, tone }) {
  const total = Number(row.price) * Number(row.quantity);
  const depth = maxQuantity ? Math.max(4, (Number(row.quantity) / maxQuantity) * 100) : 4;
  return (
    <div className={`terminalBookRow ${tone}`} style={{ '--depth': `${depth}%` }}>
      <span>{formatNumber(row.price)}</span>
      <span>{formatNumber(row.quantity)}</span>
      <span>{formatNumber(total)}</span>
    </div>
  );
}
