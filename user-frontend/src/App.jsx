import React from 'react';
import { Bitcoin, RefreshCw, ShieldCheck, Zap } from 'lucide-react';
import { AccountsPanel } from './features/auth/AccountsPanel.jsx';
import { AuthPanel } from './features/auth/AuthPanel.jsx';
import { Navbar } from './components/Navbar.jsx';
import { MarketPricesPanel } from './features/market/MarketPricesPanel.jsx';
import { OrderBookPanel } from './features/trading/OrderBookPanel.jsx';
import { OrdersPanel } from './features/trading/OrdersPanel.jsx';
import { PlaceOrderPanel } from './features/trading/PlaceOrderPanel.jsx';
import { TradesPanel } from './features/trading/TradesPanel.jsx';
import { WalletPanel } from './features/wallet/WalletPanel.jsx';
import { useExchangeConsole } from './hooks/useExchangeConsole.js';

export function App() {
  const consoleState = useExchangeConsole();

  return (
    <div className="app" id="top">
      <Navbar auth={consoleState.auth} status={consoleState.status} onLogout={consoleState.logout} />

      <main>
        <section className="heroSection">
          <div className="heroCopy">
            <div className="eyebrow"><span /> Live spot exchange</div>
            <h1>Krypto</h1>
            <p className="heroLead">Buy and sell digital assets with live market prices.</p>
            <div className="trustRow">
              <span><ShieldCheck size={17} /> Secured</span>
              <span><Zap size={17} /> Real-time</span>
              <span><Bitcoin size={17} /> 7 markets</span>
            </div>
          </div>

          <div className="heroTerminal" id="exchange">
            {consoleState.auth ? (
              <PlaceOrderPanel
                auth={consoleState.auth}
                busy={consoleState.busy}
                order={consoleState.order}
                onOrderChange={consoleState.setOrder}
                onSubmitOrder={consoleState.submitOrder}
              />
            ) : (
              <AuthPanel
                busy={consoleState.busy}
                password={consoleState.password}
                username={consoleState.username}
                onPasswordChange={consoleState.setPassword}
                onSubmit={consoleState.submitAuth}
                onUsernameChange={consoleState.setUsername}
              />
            )}
          </div>
        </section>

        <section className="marketBand" id="market">
          <div className="sectionHeading">
            <div>
              <span>Market</span>
              <h2>Live prices</h2>
            </div>
          <button
            className="iconButton"
            onClick={consoleState.refreshAll}
            disabled={!consoleState.auth || consoleState.busy}
            title="Refresh all data"
          >
            <RefreshCw size={18} />
          </button>
          </div>
          <MarketPricesPanel
            error={consoleState.marketPricesError}
            prices={consoleState.marketPrices}
            updatedAt={consoleState.marketPricesUpdatedAt}
          />
        </section>

        <section className="contentBand" id="wallet">
          <div className="sectionHeading">
            <div><span>Portfolio</span><h2>Wallet overview</h2></div>
          </div>
          <div className="grid walletGrid">
            <WalletPanel
              auth={consoleState.auth}
              balances={consoleState.balances}
              busy={consoleState.busy}
              deposit={consoleState.deposit}
              onDepositChange={consoleState.setDeposit}
              onLoadBalances={consoleState.loadBalances}
              onSubmitDeposit={consoleState.submitDeposit}
              supportedAssets={consoleState.supportedAssets}
            />
            <AccountsPanel
              accounts={consoleState.accounts}
              auth={consoleState.auth}
              busy={consoleState.busy}
              selectedAccountId={consoleState.selectedAccountId}
              onLoad={consoleState.loadAccounts}
              onSelectAccount={consoleState.selectAccount}
            />
          </div>
        </section>

        <section className="activityBand" id="orders">
          <div className="sectionHeading">
            <div><span>Exchange</span><h2>Orders and activity</h2></div>
          </div>
          <div className="grid activityGrid">
            <OrderBookPanel
              auth={consoleState.auth}
              busy={consoleState.busy}
              orderBook={consoleState.orderBook}
              onLoadOrderBook={consoleState.loadOrderBook}
            />
            <OrdersPanel
              auth={consoleState.auth}
              busy={consoleState.busy}
              orders={consoleState.orders}
              onLoadOrders={consoleState.loadOrders}
            />
            <TradesPanel
              auth={consoleState.auth}
              busy={consoleState.busy}
              trades={consoleState.trades}
              onLoadTrades={consoleState.loadTrades}
            />
          </div>
        </section>
      </main>

      <footer><span>Krypto</span><p>Spot exchange platform</p></footer>
    </div>
  );
}
