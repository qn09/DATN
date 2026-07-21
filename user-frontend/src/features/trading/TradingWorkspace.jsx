import React from 'react';
import { ArrowDownToLine } from 'lucide-react';
import { formatNumber } from '../../utils/format.js';

const tabs = ['Open orders', 'Order history', 'Trades', 'Assets', 'Deposits'];

export function TradingWorkspace(props) {
  const [activeTab, setActiveTab] = React.useState('Open orders');
  const openOrders = props.orders.filter((order) => order.status === 'OPEN' || order.status === 'PARTIALLY_FILLED');

  return (
    <section className="tradingWorkspace" id="workspace">
      <div className="workspaceTabs">
        {tabs.map((tab) => <button key={tab} className={activeTab === tab ? 'active' : ''} onClick={() => setActiveTab(tab)}>{tab}</button>)}
      </div>
      <div className="workspaceBody">
        {activeTab === 'Open orders' && <OrdersTable orders={openOrders} empty="No open orders" />}
        {activeTab === 'Order history' && <OrdersTable orders={props.orders} empty="No order history" />}
        {activeTab === 'Trades' && <TradesTable trades={props.trades} />}
        {activeTab === 'Assets' && <AssetsTable balances={props.balances} />}
        {activeTab === 'Deposits' && <DepositsView {...props} />}
      </div>
    </section>
  );
}

function OrdersTable({ orders, empty }) {
  return (
    <table className="terminalTable">
      <thead><tr><th>Time</th><th>Symbol</th><th>Side</th><th>Price</th><th>Original</th><th>Remaining</th><th>Status</th></tr></thead>
      <tbody>{orders.length ? orders.map((order) => (
        <tr key={order.id}>
          <td>{new Date(order.createdAt).toLocaleString()}</td><td>{order.symbol}</td>
          <td className={order.side === 'BUY' ? 'positive' : 'negative'}>{order.side}</td>
          <td>{formatNumber(order.price)}</td><td>{formatNumber(order.originalQuantity)}</td>
          <td>{formatNumber(order.remainingQuantity)}</td><td>{order.status}</td>
        </tr>
      )) : <EmptyRow columns={7} text={empty} />}</tbody>
    </table>
  );
}

function TradesTable({ trades }) {
  return (
    <table className="terminalTable">
      <thead><tr><th>Time</th><th>Symbol</th><th>Price</th><th>Quantity</th><th>Source</th></tr></thead>
      <tbody>{trades.length ? trades.map((trade) => (
        <tr key={trade.id}><td>{new Date(trade.occurredAt).toLocaleString()}</td><td>{trade.symbol}</td><td>{formatNumber(trade.price)}</td><td>{formatNumber(trade.quantity)}</td><td>{trade.source}</td></tr>
      )) : <EmptyRow columns={5} text="No trades for this market" />}</tbody>
    </table>
  );
}

function AssetsTable({ balances }) {
  const total = balances.reduce((sum, item) => sum + Number(item.totalValueUsdt || 0), 0);
  return (
    <div id="assets">
      <div className="assetSummary"><span>Estimated balance</span><strong>{formatNumber(total)} USDT</strong></div>
      <table className="terminalTable">
        <thead><tr><th>Asset</th><th>Available</th><th>Locked</th><th>Market price</th><th>Value (USDT)</th></tr></thead>
        <tbody>{balances.length ? balances.map((balance) => (
          <tr key={balance.asset}><td><b>{balance.asset}</b></td><td>{formatNumber(balance.available)}</td><td>{formatNumber(balance.locked)}</td><td>{formatNumber(balance.priceUsdt)}</td><td>{formatNumber(balance.totalValueUsdt)}</td></tr>
        )) : <EmptyRow columns={5} text="No assets" />}</tbody>
      </table>
    </div>
  );
}

function DepositsView({ busy, deposit, fiatDeposits, onDepositChange, onSubmitDeposit }) {
  return (
    <div className="depositsView">
      <form className="depositForm" onSubmit={onSubmitDeposit}>
        <label>Currency<input value={deposit.currency} readOnly /></label>
        <label>Amount<input type="number" min="1" value={deposit.amount} onChange={(event) => onDepositChange({ ...deposit, amount: event.target.value, idempotencyKey: '' })} /></label>
        <button disabled={busy}><ArrowDownToLine size={15} /> Create deposit</button>
      </form>
      <table className="terminalTable">
        <thead><tr><th>Created</th><th>Reference</th><th>Amount</th><th>Gateway</th><th>Status</th></tr></thead>
        <tbody>{fiatDeposits.length ? fiatDeposits.map((item) => (
          <tr key={item.requestId}><td>{new Date(item.createdAt).toLocaleString()}</td><td>{item.gatewayReference || item.requestId.slice(0, 12)}</td><td>{formatNumber(item.amount)} {item.currency}</td><td>{item.gateway}</td><td><span className={`depositStatus ${item.status.toLowerCase()}`}>{item.status}</span></td></tr>
        )) : <EmptyRow columns={5} text="No deposit requests" />}</tbody>
      </table>
    </div>
  );
}

function EmptyRow({ columns, text }) {
  return <tr><td className="terminalEmptyCell" colSpan={columns}>{text}</td></tr>;
}
