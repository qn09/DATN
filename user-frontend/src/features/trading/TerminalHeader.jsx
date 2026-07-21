import { BarChart3, Bell, LogOut, Search, Wallet } from 'lucide-react';

export function TerminalHeader({ account, marketPrices, selectedSymbol, status, onLogout, onSelectSymbol }) {
  return (
    <header className="terminalHeader">
      <a className="terminalBrand" href="#terminal" aria-label="Krypto terminal">
        <span className="brandGlyph"><BarChart3 size={18} /></span>
        <strong>KRYPTO</strong>
      </a>
      <nav className="terminalNav" aria-label="Trading navigation">
        <a className="active" href="#terminal">Trade</a>
        <a href="#workspace">Orders</a>
        <a href="#assets">Assets</a>
      </nav>
      <label className="marketSearch">
        <Search size={15} />
        <select value={selectedSymbol} onChange={(event) => onSelectSymbol(event.target.value)}>
          {marketPrices.map((item) => <option key={item.symbol} value={item.symbol}>{item.symbol}</option>)}
          {marketPrices.length === 0 && <option value={selectedSymbol}>{selectedSymbol}</option>}
        </select>
      </label>
      <div className="terminalActions">
        <span className={`syncState ${status.type}`} title={status.text}><i /> {status.text}</span>
        <button className="iconControl" title="Notifications" aria-label="Notifications"><Bell size={17} /></button>
        <span className="accountName"><Wallet size={15} /> {account.username}</span>
        <button className="iconControl" onClick={onLogout} title="Sign out" aria-label="Sign out"><LogOut size={17} /></button>
      </div>
    </header>
  );
}
