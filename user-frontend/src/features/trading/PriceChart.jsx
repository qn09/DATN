import React from 'react';
import { CandlestickSeries, ColorType, createChart, HistogramSeries } from 'lightweight-charts';
import { BarChart2, CandlestickChart } from 'lucide-react';
import { formatNumber } from '../../utils/format.js';

const ranges = [
  { label: '1m', value: '1m' },
  { label: '5m', value: '5m' },
  { label: '15m', value: '15m' },
  { label: '1h', value: '1h' },
  { label: '4h', value: '4h' },
  { label: '1D', value: '1d' }
];

export function PriceChart({ interval, klines, price, symbol, onIntervalChange }) {
  const containerRef = React.useRef(null);
  const chartRef = React.useRef(null);
  const candleSeriesRef = React.useRef(null);
  const volumeSeriesRef = React.useRef(null);
  const fittedKeyRef = React.useRef('');

  React.useEffect(() => {
    if (!containerRef.current) return undefined;
    const chart = createChart(containerRef.current, {
      autoSize: true,
      layout: { background: { type: ColorType.Solid, color: '#080a0c' }, textColor: '#75808a' },
      grid: { vertLines: { color: '#171b20' }, horzLines: { color: '#171b20' } },
      rightPriceScale: { borderColor: '#252a30', scaleMargins: { top: 0.08, bottom: 0.24 } },
      timeScale: { borderColor: '#252a30', timeVisible: true, secondsVisible: false },
      crosshair: { vertLine: { color: '#737d87' }, horzLine: { color: '#737d87' } },
      handleScroll: true,
      handleScale: true
    });
    const candles = chart.addSeries(CandlestickSeries, {
      upColor: '#2fbe72', downColor: '#ef4d64', borderVisible: false,
      wickUpColor: '#2fbe72', wickDownColor: '#ef4d64', priceLineColor: '#2fbe72'
    });
    const volume = chart.addSeries(HistogramSeries, {
      priceFormat: { type: 'volume' }, priceScaleId: '', lastValueVisible: false, priceLineVisible: false
    });
    volume.priceScale().applyOptions({ scaleMargins: { top: 0.82, bottom: 0 } });
    chartRef.current = chart;
    candleSeriesRef.current = candles;
    volumeSeriesRef.current = volume;
    return () => {
      chart.remove();
      chartRef.current = null;
      candleSeriesRef.current = null;
      volumeSeriesRef.current = null;
    };
  }, []);

  React.useEffect(() => {
    if (!candleSeriesRef.current || !volumeSeriesRef.current || !klines.length) return;
    const candles = klines.map((item) => ({
      time: Math.floor(item.openTime / 1000),
      open: Number(item.open), high: Number(item.high), low: Number(item.low), close: Number(item.close)
    }));
    const volumes = klines.map((item) => ({
      time: Math.floor(item.openTime / 1000),
      value: Number(item.volume),
      color: Number(item.close) >= Number(item.open) ? 'rgba(47,190,114,.35)' : 'rgba(239,77,100,.35)'
    }));
    candleSeriesRef.current.setData(candles);
    volumeSeriesRef.current.setData(volumes);
    const key = `${symbol}:${interval}`;
    if (fittedKeyRef.current !== key) {
      chartRef.current.timeScale().fitContent();
      fittedKeyRef.current = key;
    }
  }, [interval, klines, symbol]);

  return (
    <section className="chartPanel">
      <header className="terminalPanelHeader">
        <div className="panelTabs"><button className="active">Chart</button><button>Trading data</button></div>
        <span className="chartQuote">{symbol} <b>{formatNumber(price)}</b></span>
      </header>
      <div className="chartToolbar">
        <div className="rangeControls">
          {ranges.map((item) => (
            <button key={item.value} className={interval === item.value ? 'active' : ''} onClick={() => onIntervalChange(item.value)}>
              {item.label}
            </button>
          ))}
        </div>
        <span><CandlestickChart size={15} /> Candles</span>
        <span><BarChart2 size={15} /> Volume</span>
      </div>
      <div className="chartCanvas" ref={containerRef} />
      {!klines.length && <div className="chartWaiting">Loading Binance candles...</div>}
    </section>
  );
}
