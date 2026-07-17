import { BarChart3, RefreshCw } from 'lucide-react';
import { Panel } from '../../components/Panel.jsx';
import { formatNumber } from '../../utils/format.js';

export function OrderBookPanel({ auth, busy, orderBook, onLoadOrderBook }) {
  return (
    <Panel title="Order Book" icon={<BarChart3 size={18} />}>
      <button className="ghostButton" onClick={onLoadOrderBook} disabled={!auth || busy}><RefreshCw size={15} /> Load order book</button>
      <div className="book">
        <BookSide title="Bids" rows={orderBook?.bids || []} tone="buy" />
        <BookSide title="Asks" rows={orderBook?.asks || []} tone="sell" />
      </div>
    </Panel>
  );
}

function BookSide({ title, rows, tone }) {
  return (
    <div className="bookSide">
      <h4>{title}</h4>
      {rows.length === 0 ? <p className="empty">Empty</p> : rows.map((row) => (
        <div key={row.id} className="bookRow">
          <span className={tone}>{formatNumber(row.price)}</span>
          <span>{formatNumber(row.remainingQuantity)}</span>
        </div>
      ))}
    </div>
  );
}
