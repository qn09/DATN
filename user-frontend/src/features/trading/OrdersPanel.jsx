import { ListOrdered, RefreshCw } from 'lucide-react';
import { Panel } from '../../components/Panel.jsx';
import { formatNumber } from '../../utils/format.js';

export function OrdersPanel({ auth, busy, orders, onLoadOrders }) {
  return (
    <Panel title="Orders" icon={<ListOrdered size={18} />}>
      <button className="ghostButton" onClick={onLoadOrders} disabled={!auth || busy}><RefreshCw size={15} /> Load orders</button>
      <table>
        <thead>
          <tr><th>ID</th><th>Side</th><th>Price</th><th>Remain</th><th>Status</th></tr>
        </thead>
        <tbody>
          {orders.length === 0 ? (
            <tr><td colSpan="5" className="emptyCell">No orders loaded</td></tr>
          ) : orders.map((item) => (
            <tr key={item.id}>
              <td>#{item.id}</td>
              <td><span className={`pill ${item.side.toLowerCase()}`}>{item.side}</span></td>
              <td>{formatNumber(item.price)}</td>
              <td>{formatNumber(item.remainingQuantity)}</td>
              <td>{item.status}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </Panel>
  );
}
