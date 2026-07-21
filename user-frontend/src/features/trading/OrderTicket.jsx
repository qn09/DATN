import { Info } from 'lucide-react';
import { formatNumber } from '../../utils/format.js';

const allocations = [25, 50, 75, 100];

export function OrderTicket({ balances, busy, marketPrice, order, onOrderChange, onSubmit }) {
  const [base, quote] = order.symbol.split('-');
  const availableAsset = order.side === 'BUY' ? quote : base;
  const available = Number(balances.find((item) => item.asset === availableAsset)?.available || 0);
  const total = Number(order.price || 0) * Number(order.quantity || 0);

  function allocate(percent) {
    const price = Number(order.price || marketPrice || 0);
    const quantity = order.side === 'BUY'
      ? (price ? (available * percent / 100) / price : 0)
      : available * percent / 100;
    onOrderChange({ ...order, quantity: quantity ? quantity.toFixed(8).replace(/0+$/, '').replace(/\.$/, '') : '' });
  }

  return (
    <aside className="orderTicket">
      <header className="terminalPanelHeader">
        <div className="panelTabs"><button className="active">Trade</button><button>Tools</button></div>
        <span className="spotLabel">Spot</span>
      </header>
      <form onSubmit={onSubmit}>
        <div className="sideSwitch">
          <button type="button" className={order.side === 'BUY' ? 'active buySide' : ''} onClick={() => onOrderChange({ ...order, side: 'BUY' })}>Buy</button>
          <button type="button" className={order.side === 'SELL' ? 'active sellSide' : ''} onClick={() => onOrderChange({ ...order, side: 'SELL' })}>Sell</button>
        </div>
        <div className="orderTypeTabs"><button type="button" className="active">Limit</button><button type="button">Market</button><button type="button">TP/SL</button><Info size={13} /></div>
        <label className="tradeField">
          <span>Price</span>
          <div><input type="number" min="0" step="any" value={order.price} onChange={(event) => onOrderChange({ ...order, price: event.target.value })} /><b>{quote}</b></div>
        </label>
        <button type="button" className="marketPriceButton" onClick={() => onOrderChange({ ...order, price: String(marketPrice || '') })}>Use market price {formatNumber(marketPrice)}</button>
        <label className="tradeField">
          <span>Amount</span>
          <div><input type="number" min="0" step="any" value={order.quantity} onChange={(event) => onOrderChange({ ...order, quantity: event.target.value })} /><b>{base}</b></div>
        </label>
        <div className="allocationTrack">
          {allocations.map((percent) => <button type="button" key={percent} onClick={() => allocate(percent)}><i />{percent}%</button>)}
        </div>
        <label className="tradeField totalField">
          <span>Total</span>
          <div><input readOnly value={total ? total.toFixed(8).replace(/0+$/, '').replace(/\.$/, '') : ''} /><b>{quote}</b></div>
        </label>
        <div className="availableLine"><span>Available</span><strong>{formatNumber(available)} {availableAsset}</strong></div>
        <button className={`submitTrade ${order.side === 'SELL' ? 'sellSubmit' : ''}`} disabled={busy || !Number(order.quantity) || !Number(order.price)}>
          {busy ? 'Processing...' : `${order.side === 'BUY' ? 'Buy' : 'Sell'} ${base}`}
        </button>
      </form>
    </aside>
  );
}
