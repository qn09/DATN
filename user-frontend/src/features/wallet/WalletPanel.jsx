import { ArrowDownToLine, RefreshCw, Wallet } from 'lucide-react';
import { Panel } from '../../components/Panel.jsx';
import { formatNumber } from '../../utils/format.js';

export function WalletPanel({
  auth,
  balances,
  busy,
  deposit,
  onDepositChange,
  onLoadBalances,
  onSubmitDeposit,
  supportedAssets
}) {
  const portfolioValue = balances.reduce((sum, balance) => sum + Number(balance.totalValueUsdt || 0), 0);

  return (
    <Panel title="Wallet" icon={<Wallet size={18} />}>
      <form onSubmit={onSubmitDeposit} className="form inlineForm">
        <label>
          Asset
          <select value={deposit.asset} onChange={(event) => onDepositChange({ ...deposit, asset: event.target.value })}>
            {supportedAssets.map((asset) => <option key={asset} value={asset}>{asset}</option>)}
          </select>
        </label>
        <label>
          Amount
          <input type="number" value={deposit.amount} onChange={(event) => onDepositChange({ ...deposit, amount: event.target.value })} />
        </label>
        <button className="secondaryButton" type="submit" disabled={!auth || busy}><ArrowDownToLine size={15} /> Deposit</button>
      </form>
      <button className="ghostButton" onClick={onLoadBalances} disabled={!auth || busy}><RefreshCw size={15} /> Load balances</button>
      <div className="portfolioSummary">
        <span>Estimated portfolio value</span>
        <strong>{formatNumber(portfolioValue)} USDT</strong>
      </div>
      <table>
        <thead>
          <tr><th>Asset</th><th>Available</th><th>Locked</th><th>Price</th><th>Value</th></tr>
        </thead>
        <tbody>
          {balances.length === 0 ? (
            <tr><td colSpan="5" className="emptyCell">No balances loaded</td></tr>
          ) : balances.map((balance) => (
            <tr key={balance.asset}>
              <td>{balance.asset}</td>
              <td>{formatNumber(balance.available)}</td>
              <td>{formatNumber(balance.locked)}</td>
              <td>{formatNumber(balance.priceUsdt)} USDT</td>
              <td>{formatNumber(balance.totalValueUsdt)} USDT</td>
            </tr>
          ))}
        </tbody>
      </table>
    </Panel>
  );
}
