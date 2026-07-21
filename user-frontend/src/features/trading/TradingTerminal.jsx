import { TerminalHeader } from './TerminalHeader.jsx';
import { MarketStrip } from './MarketStrip.jsx';
import { PriceChart } from './PriceChart.jsx';
import { TerminalOrderBook } from './TerminalOrderBook.jsx';
import { OrderTicket } from './OrderTicket.jsx';
import { TradingWorkspace } from './TradingWorkspace.jsx';

export function TradingTerminal({ exchange }) {
  const selectedPrice = exchange.marketPrices.find((item) => item.symbol === exchange.order.symbol);

  return (
    <div className="terminalApp">
      <TerminalHeader
        account={exchange.auth.account}
        marketPrices={exchange.marketPrices}
        selectedSymbol={exchange.order.symbol}
        status={exchange.status}
        onLogout={exchange.logout}
        onSelectSymbol={exchange.selectSymbol}
      />
      <MarketStrip
        klines={exchange.klines}
        price={selectedPrice}
        trades={exchange.trades}
        updatedAt={exchange.marketPricesUpdatedAt}
      />

      <main className="terminalGrid">
        <PriceChart
          interval={exchange.chartInterval}
          klines={exchange.klines}
          price={selectedPrice?.price}
          symbol={exchange.order.symbol}
          onIntervalChange={exchange.setChartInterval}
        />
        <TerminalOrderBook
          connected={exchange.marketStreamConnected}
          orderBook={exchange.orderBook}
          symbol={exchange.order.symbol}
        />
        <OrderTicket
          balances={exchange.balances}
          busy={exchange.busy}
          marketPrice={selectedPrice?.price}
          order={exchange.order}
          onOrderChange={exchange.setOrder}
          onSubmit={exchange.submitOrder}
        />
      </main>

      <TradingWorkspace
        balances={exchange.balances}
        busy={exchange.busy}
        deposit={exchange.deposit}
        fiatDeposits={exchange.fiatDeposits}
        orders={exchange.orders}
        trades={exchange.trades}
        onDepositChange={exchange.setDeposit}
        onSubmitDeposit={exchange.submitDeposit}
      />
    </div>
  );
}
