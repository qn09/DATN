import { Radio, TrendingUp } from 'lucide-react';
import { Panel } from '../../components/Panel.jsx';
import { formatNumber } from '../../utils/format.js';

export function MarketPricesPanel({ error, prices, updatedAt }) {
  return (
    <Panel title="Live Market Prices" icon={<TrendingUp size={18} />}>
      <div className={`liveTicker ${error ? 'error' : ''}`}>
        <Radio size={15} />
        <span>{error || `Auto refresh ${updatedAt ? new Date(updatedAt).toLocaleTimeString() : 'starting'}`}</span>
      </div>
      <table>
        <thead>
          <tr><th>Symbol</th><th>Price</th><th>Source</th></tr>
        </thead>
        <tbody>
          {prices.length === 0 ? (
            <tr><td colSpan="3" className="emptyCell">No live prices loaded</td></tr>
          ) : prices.map((item) => (
            <tr key={item.symbol}>
              <td>{item.symbol}</td>
              <td>{formatNumber(item.price)} {item.quoteAsset}</td>
              <td>{item.source}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </Panel>
  );
}
