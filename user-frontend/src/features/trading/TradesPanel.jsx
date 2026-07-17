import { Activity, RefreshCw } from 'lucide-react';
import { Panel } from '../../components/Panel.jsx';
import { formatNumber } from '../../utils/format.js';

export function TradesPanel({ auth, busy, trades, onLoadTrades }) {
  return (
    <Panel title="Trades" icon={<Activity size={18} />}>
      <button className="ghostButton" onClick={onLoadTrades} disabled={!auth || busy}><RefreshCw size={15} /> Load trades</button>
      <table>
        <thead>
          <tr><th>ID</th><th>Price</th><th>Qty</th><th>Orders</th><th>Source</th></tr>
        </thead>
        <tbody>
          {trades.length === 0 ? (
            <tr><td colSpan="5" className="emptyCell">No trades loaded</td></tr>
          ) : trades.map((trade) => (
            <tr key={trade.id}>
              <td>#{trade.id}</td>
              <td>{formatNumber(trade.price)}</td>
              <td>{formatNumber(trade.quantity)}</td>
              <td>{trade.buyOrderId || '-'}/{trade.sellOrderId || '-'}</td>
              <td>{trade.source}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </Panel>
  );
}
