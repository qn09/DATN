import React from 'react';
import { apiRequest } from '../api/client.js';

const initialOrder = {
  accountId: '',
  symbol: 'BTC-USDT',
  side: 'BUY',
  price: '',
  quantity: ''
};

const marketRefreshMs = 3000;
const accountRefreshMs = 5000;

function readStoredAuth() {
  try {
    const saved = localStorage.getItem('exchange.auth');
    return saved ? JSON.parse(saved) : null;
  } catch {
    localStorage.removeItem('exchange.auth');
    return null;
  }
}

export function useExchangeConsole() {
  const [auth, setAuth] = React.useState(readStoredAuth);
  const [username, setUsername] = React.useState('api_buyer_01');
  const [password, setPassword] = React.useState('secret123');
  const [selectedAccountId, setSelectedAccountId] = React.useState(() => auth?.account?.id ?? '');
  const [deposit, setDeposit] = React.useState({ currency: 'VND', amount: '1000000', idempotencyKey: '' });
  const [fiatDeposits, setFiatDeposits] = React.useState([]);
  const [order, setOrder] = React.useState(() => ({ ...initialOrder, accountId: auth?.account?.id ?? '' }));
  const [balances, setBalances] = React.useState([]);
  const [orders, setOrders] = React.useState([]);
  const [orderBook, setOrderBook] = React.useState(null);
  const [trades, setTrades] = React.useState([]);
  const [marketPrices, setMarketPrices] = React.useState([]);
  const [klines, setKlines] = React.useState([]);
  const [chartInterval, setChartInterval] = React.useState('15m');
  const [marketPricesUpdatedAt, setMarketPricesUpdatedAt] = React.useState(null);
  const [marketPricesError, setMarketPricesError] = React.useState('');
  const [marketStreamConnected, setMarketStreamConnected] = React.useState(false);
  const [status, setStatus] = React.useState({ type: 'idle', text: 'Connected' });
  const [busy, setBusy] = React.useState(false);

  const request = React.useCallback((path, options = {}) => (
    apiRequest(path, { ...options, token: auth?.token })
  ), [auth?.token]);

  const syncMarketPrices = React.useCallback(async () => {
    try {
      const data = await request('/market/prices');
      setMarketPrices(data);
      setMarketPricesUpdatedAt(new Date().toISOString());
      setMarketPricesError('');
      const selected = data.find((item) => item.symbol === order.symbol);
      if (selected) {
        setOrder((current) => current.price ? current : { ...current, price: String(selected.price) });
      }
    } catch (error) {
      setMarketPricesError(error.message);
    }
  }, [order.symbol, request]);

  const syncKlines = React.useCallback(async () => {
    const data = await request(`/market/klines/${order.symbol}?interval=${chartInterval}&limit=300`);
    setKlines(data);
  }, [chartInterval, order.symbol, request]);

  const syncOrderBook = React.useCallback(async () => {
    const data = await request(`/market/depth/${order.symbol}?limit=20`);
    setOrderBook(data);
  }, [order.symbol, request]);

  const syncTrades = React.useCallback(async () => {
    const data = await request(`/trades/${order.symbol}`);
    setTrades(data);
  }, [order.symbol, request]);

  const syncAccount = React.useCallback(async () => {
    if (!selectedAccountId) return;
    const [balanceResult, orderResult, depositResult] = await Promise.allSettled([
      request(`/accounts/${selectedAccountId}/balances`),
      request(`/orders?accountId=${selectedAccountId}`),
      request(`/fiat-deposits/accounts/${selectedAccountId}?limit=20`)
    ]);
    if (balanceResult.status === 'fulfilled') setBalances(balanceResult.value);
    if (orderResult.status === 'fulfilled') setOrders(orderResult.value);
    if (depositResult.status === 'fulfilled') setFiatDeposits(depositResult.value);
  }, [request, selectedAccountId]);

  React.useEffect(() => {
    if (!auth?.account?.id) return;
    const accountId = String(auth.account.id);
    setSelectedAccountId(accountId);
    setOrder((current) => ({ ...current, accountId }));
  }, [auth?.account?.id]);

  React.useEffect(() => {
    if (!auth?.token) return undefined;
    setMarketStreamConnected(false);
    let stopped = false;
    let timer;
    async function pollMarket() {
      await Promise.allSettled([syncMarketPrices(), syncKlines(), syncTrades()]);
      if (!stopped) timer = window.setTimeout(pollMarket, marketRefreshMs);
    }
    pollMarket();
    return () => { stopped = true; window.clearTimeout(timer); };
  }, [auth?.token, syncKlines, syncMarketPrices, syncTrades]);

  React.useEffect(() => {
    if (!auth?.token) return undefined;
    let stopped = false;
    let socket;
    let reconnectTimer;
    let reconnectDelay = 1000;

    async function connect() {
      await syncOrderBook().catch(() => undefined);
      if (stopped) return;
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      socket = new WebSocket(`${protocol}//${window.location.host}/ws/market`);
      socket.onopen = () => {
        reconnectDelay = 1000;
        socket.send(JSON.stringify({ type: 'AUTH', token: auth.token }));
      };
      socket.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          if (message.type === 'AUTHENTICATED') {
            socket.send(JSON.stringify({ type: 'SUBSCRIBE', symbol: order.symbol }));
          } else if (message.type === 'DEPTH' && message.data?.symbol === order.symbol) {
            setOrderBook(message.data);
            setMarketStreamConnected(true);
          }
        } catch {
          socket.close();
        }
      };
      socket.onerror = () => socket.close();
      socket.onclose = () => {
        setMarketStreamConnected(false);
        if (!stopped) {
          reconnectTimer = window.setTimeout(connect, reconnectDelay);
          reconnectDelay = Math.min(reconnectDelay * 2, 10000);
        }
      };
    }

    connect();
    return () => {
      stopped = true;
      window.clearTimeout(reconnectTimer);
      if (socket && socket.readyState < WebSocket.CLOSING) socket.close();
    };
  }, [auth?.token, order.symbol, syncOrderBook]);

  React.useEffect(() => {
    if (!auth?.token || !selectedAccountId) return undefined;
    let stopped = false;
    let timer;
    async function pollAccount() {
      await syncAccount();
      if (!stopped) timer = window.setTimeout(pollAccount, accountRefreshMs);
    }
    pollAccount();
    return () => { stopped = true; window.clearTimeout(timer); };
  }, [auth?.token, selectedAccountId, syncAccount]);

  async function run(label, action) {
    setBusy(true);
    setStatus({ type: 'idle', text: label });
    try {
      const result = await action();
      setStatus({ type: 'success', text: `${label} completed` });
      return result;
    } catch (error) {
      setStatus({ type: 'error', text: error.message });
      return null;
    } finally {
      setBusy(false);
    }
  }

  async function submitAuth(event) {
    event.preventDefault();
    const result = await run('Sign in', () => apiRequest('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    }));
    if (result) {
      setAuth(result);
      localStorage.setItem('exchange.auth', JSON.stringify(result));
    }
  }

  function logout() {
    setAuth(null);
    setBalances([]);
    setFiatDeposits([]);
    setOrders([]);
    setOrderBook(null);
    setTrades([]);
    setMarketPrices([]);
    setKlines([]);
    setMarketStreamConnected(false);
    localStorage.removeItem('exchange.auth');
    setStatus({ type: 'idle', text: 'Connected' });
  }

  function selectSymbol(symbol) {
    const price = marketPrices.find((item) => item.symbol === symbol)?.price;
    setKlines([]);
    setOrder((current) => ({ ...current, symbol, price: price ? String(price) : '' }));
  }

  async function submitDeposit(event) {
    event.preventDefault();
    const idempotencyKey = deposit.idempotencyKey || crypto.randomUUID();
    const data = await run('Create deposit', async () => {
      const created = await request('/fiat-deposits', {
        method: 'POST',
        headers: { 'Idempotency-Key': idempotencyKey },
        body: JSON.stringify({
          accountId: Number(selectedAccountId),
          currency: deposit.currency,
          amount: Number(deposit.amount)
        })
      });
      return request(`/fiat-deposits/${created.requestId}/submit`, { method: 'POST' });
    });
    if (data) {
      setDeposit((current) => ({ ...current, idempotencyKey: '' }));
      setFiatDeposits((current) => [data, ...current.filter((item) => item.requestId !== data.requestId)]);
      await syncAccount();
    }
  }

  async function submitOrder(event) {
    event.preventDefault();
    const data = await run('Place order', () => request('/orders', {
      method: 'POST',
      body: JSON.stringify({
        ...order,
        accountId: Number(order.accountId),
        price: Number(order.price),
        quantity: Number(order.quantity)
      })
    }));
    if (data) {
      setOrder((current) => ({ ...current, quantity: '' }));
      await Promise.allSettled([syncTrades(), syncAccount()]);
    }
  }

  return {
    auth,
    balances,
    busy,
    deposit,
    fiatDeposits,
    chartInterval,
    klines,
    logout,
    marketPrices,
    marketPricesError,
    marketPricesUpdatedAt,
    marketStreamConnected,
    order,
    orderBook,
    orders,
    password,
    selectedAccountId,
    selectSymbol,
    setDeposit,
    setChartInterval,
    setOrder,
    setPassword,
    setUsername,
    status,
    submitAuth,
    submitDeposit,
    submitOrder,
    trades,
    username
  };
}
