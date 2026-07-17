import { CircleDollarSign, Send } from 'lucide-react';
import { Panel } from '../../components/Panel.jsx';

export function PlaceOrderPanel({ auth, busy, order, onOrderChange, onSubmitOrder }) {
  return (
    <Panel title="Place Order" icon={<Send size={18} />}>
      <form onSubmit={onSubmitOrder} className="form">
        <div className="twoCols">
          <label>
            Account ID
            <input type="number" value={order.accountId} onChange={(event) => onOrderChange({ ...order, accountId: event.target.value })} />
          </label>
          <label>
            Symbol
            <input value={order.symbol} onChange={(event) => onOrderChange({ ...order, symbol: event.target.value.toUpperCase() })} />
          </label>
        </div>
        <div className="segmented">
          <button type="button" className={order.side === 'BUY' ? 'active buy' : ''} onClick={() => onOrderChange({ ...order, side: 'BUY' })}>BUY</button>
          <button type="button" className={order.side === 'SELL' ? 'active sell' : ''} onClick={() => onOrderChange({ ...order, side: 'SELL' })}>SELL</button>
        </div>
        <div className="twoCols">
          <label>
            Price
            <input type="number" value={order.price} onChange={(event) => onOrderChange({ ...order, price: event.target.value })} />
          </label>
          <label>
            Quantity
            <input type="number" value={order.quantity} onChange={(event) => onOrderChange({ ...order, quantity: event.target.value })} />
          </label>
        </div>
        <button className="primaryButton" type="submit" disabled={!auth || busy}><CircleDollarSign size={16} /> Submit order</button>
      </form>
    </Panel>
  );
}
