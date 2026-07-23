import {
  Activity,
  ArrowDownToLine,
  BarChart3,
  Check,
  Database,
  KeyRound,
  ListOrdered,
  Lock,
  RefreshCw,
  ShieldCheck,
  TrendingUp,
  Users,
  Wallet,
  X
} from 'lucide-react';
import { MetricCard } from './components/MetricCard.jsx';
import { Panel } from './components/Panel.jsx';
import { useAdminDashboard } from './hooks/useAdminDashboard.js';
import { formatNumber, formatTime } from './utils/format.js';

export function App() {
  const state = useAdminDashboard();

  return (
    <div className="adminShell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brandMark"><ShieldCheck size={22} /></div>
          <div>
            <h1>Exchange Admin</h1>
            <p>Operations control center</p>
          </div>
        </div>

        <div className="sessionBox">
          <div className="sessionHeader">
            <KeyRound size={17} />
            <span>Admin Session</span>
          </div>
          {state.auth ? (
            <>
              <div className="identity">
                <strong>{state.auth.account.username}</strong>
                <span>Account #{state.auth.account.id} · {state.auth.account.role}</span>
              </div>
              <button className="ghostButton" onClick={state.logout}><Lock size={16} /> Sign out</button>
            </>
          ) : (
            <p className="muted">Login with an ADMIN account.</p>
          )}
        </div>

        <div className={`status ${state.status.type}`}>
          <Activity size={16} />
          <span>{state.status.text}</span>
        </div>
      </aside>

      <main className="workspace">
        <section className="topbar">
          <div>
            <h2>Admin Dashboard</h2>
            <p>Accounts, orders, trades, and market monitoring</p>
          </div>
          <button
            className="iconButton"
            onClick={state.refreshDashboard}
            disabled={!state.auth || state.busy}
            title="Refresh dashboard"
          >
            <RefreshCw size={18} />
          </button>
        </section>

        {!state.auth ? (
          <section className="loginPane">
            <Panel title="Admin Login" icon={<KeyRound size={18} />}>
              <form onSubmit={state.login} className="form">
                <label>
                  Username
                  <input value={state.username} onChange={(event) => state.setUsername(event.target.value)} />
                </label>
                <label>
                  Password
                  <input type="password" value={state.password} onChange={(event) => state.setPassword(event.target.value)} />
                </label>
                <button className="primaryButton" type="submit" disabled={state.busy}>
                  <ShieldCheck size={16} /> Login as admin
                </button>
              </form>
            </Panel>
          </section>
        ) : (
          <>
            <section className="metricsGrid">
              <MetricCard label="Accounts" value={state.summary?.accountCount ?? 0} icon={<Users size={18} />} />
              <MetricCard label="Orders" value={state.summary?.orderCount ?? 0} icon={<ListOrdered size={18} />} />
              <MetricCard label="Open Orders" value={state.summary?.openOrderCount ?? 0} icon={<Database size={18} />} />
              <MetricCard label="Trades" value={state.summary?.tradeCount ?? 0} icon={<BarChart3 size={18} />} />
              <MetricCard label="USDT Liability" value={state.summary?.stablecoinLiabilityUsdt ?? 0} suffix="USDT" icon={<Wallet size={18} />} />
            </section>

            <section className="grid twoCol">
              <Panel title="Live Market Prices" icon={<TrendingUp size={18} />}>
                <table>
                  <thead>
                    <tr><th>Symbol</th><th>Price</th><th>Source</th></tr>
                  </thead>
                  <tbody>
                    {state.prices.length === 0 ? (
                      <tr><td colSpan="3" className="emptyCell">No prices loaded</td></tr>
                    ) : state.prices.map((price) => (
                      <tr key={price.symbol}>
                        <td>{price.symbol}</td>
                        <td>{formatNumber(price.price)} {price.quoteAsset}</td>
                        <td>{price.source}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Panel>

              <Panel title="Accounts" icon={<Users size={18} />}>
                <table>
                  <thead>
                    <tr><th>ID</th><th>Username</th><th>Role</th></tr>
                  </thead>
                  <tbody>
                    {state.accounts.length === 0 ? (
                      <tr><td colSpan="3" className="emptyCell">No accounts loaded</td></tr>
                    ) : state.accounts.map((account) => (
                      <tr key={account.id}>
                        <td>#{account.id}</td>
                        <td>{account.username}</td>
                        <td><span className={`rolePill ${account.role.toLowerCase()}`}>{account.role}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Panel>
            </section>

            <section className="grid">
              <Panel title="Domestic Deposits" icon={<ArrowDownToLine size={18} />}>
                <table>
                  <thead>
                    <tr><th>Reference</th><th>Account</th><th>Amount</th><th>Gateway</th><th>Status</th><th>Created</th><th>Decision</th></tr>
                  </thead>
                  <tbody>
                    {state.fiatDeposits.length === 0 ? (
                      <tr><td colSpan="7" className="emptyCell">No fiat deposits</td></tr>
                    ) : state.fiatDeposits.map((deposit) => (
                      <tr key={deposit.requestId}>
                        <td title={deposit.gatewayReference || deposit.requestId}>
                          {(deposit.gatewayReference || deposit.requestId).slice(0, 18)}
                        </td>
                        <td>#{deposit.accountId} {deposit.username}</td>
                        <td>{formatNumber(deposit.amount)} {deposit.currency}</td>
                        <td>{deposit.gateway}</td>
                        <td><span className={`depositStatus ${deposit.status.toLowerCase()}`}>{deposit.status}</span></td>
                        <td>{formatTime(deposit.createdAt)}</td>
                        <td>
                          {deposit.status === 'PROCESSING' ? (
                            <div className="depositDecision">
                              <button
                                className="decisionButton approve"
                                title="Approve deposit"
                                disabled={state.busy}
                                onClick={() => state.decideDeposit(deposit.requestId, 'SUCCESS')}
                              >
                                <Check size={15} />
                              </button>
                              <input
                                aria-label={`Rejection reason for ${deposit.requestId}`}
                                placeholder="Rejection reason"
                                value={state.depositFailureReasons[deposit.requestId] || ''}
                                onChange={(event) => state.setDepositFailureReason(deposit.requestId, event.target.value)}
                              />
                              <button
                                className="decisionButton reject"
                                title="Reject deposit"
                                disabled={state.busy || !(state.depositFailureReasons[deposit.requestId] || '').trim()}
                                onClick={() => state.decideDeposit(deposit.requestId, 'FAILED')}
                              >
                                <X size={15} />
                              </button>
                            </div>
                          ) : (
                            <span className="decisionResult">{deposit.failureReason || 'Completed'}</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Panel>
            </section>

            <section className="grid twoCol">
              <Panel title="Orders" icon={<ListOrdered size={18} />}>
                <table>
                  <thead>
                    <tr><th>ID</th><th>Account</th><th>Symbol</th><th>Side</th><th>Price</th><th>Remain</th><th>Status</th></tr>
                  </thead>
                  <tbody>
                    {state.orders.length === 0 ? (
                      <tr><td colSpan="7" className="emptyCell">No orders loaded</td></tr>
                    ) : state.orders.map((order) => (
                      <tr key={order.id}>
                        <td>#{order.id}</td>
                        <td>#{order.accountId}</td>
                        <td>{order.symbol}</td>
                        <td><span className={`sidePill ${order.side.toLowerCase()}`}>{order.side}</span></td>
                        <td>{formatNumber(order.price)}</td>
                        <td>{formatNumber(order.remainingQuantity)}</td>
                        <td>{order.status}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Panel>

              <Panel title="Recent Trades" icon={<BarChart3 size={18} />}>
                <table>
                  <thead>
                    <tr><th>ID</th><th>Symbol</th><th>Price</th><th>Qty</th><th>Source</th><th>Time</th></tr>
                  </thead>
                  <tbody>
                    {state.trades.length === 0 ? (
                      <tr><td colSpan="6" className="emptyCell">No trades loaded</td></tr>
                    ) : state.trades.map((trade) => (
                      <tr key={trade.id}>
                        <td>#{trade.id}</td>
                        <td>{trade.symbol}</td>
                        <td>{formatNumber(trade.price)}</td>
                        <td>{formatNumber(trade.quantity)}</td>
                        <td>{trade.source}</td>
                        <td>{formatTime(trade.occurredAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Panel>
            </section>
          </>
        )}
      </main>
    </div>
  );
}
