import React from 'react';
import { apiRequest } from '../api/client.js';

const initialOrder = {
  accountId: '',
  symbol: 'BTC-USDT',
  side: 'BUY',
  price: '30000',
  quantity: '1'
};

const supportedAssets = ['USDT', 'BTC', 'ETH', 'BNB', 'SOL', 'XRP', 'ADA', 'DOGE'];
const marketPriceRefreshMs = 3000;

export function useExchangeConsole() {
  const [auth, setAuth] = React.useState(() => {
    const saved = localStorage.getItem('exchange.auth');
    return saved ? JSON.parse(saved) : null;
  });
  const [username, setUsername] = React.useState('api_buyer_01');
  const [password, setPassword] = React.useState('secret123');
  const [selectedAccountId, setSelectedAccountId] = React.useState(() => auth?.account?.id ?? '');
  const [deposit, setDeposit] = React.useState({ asset: 'USDT', amount: '100000' });
  const [order, setOrder] = React.useState(initialOrder);
  const [accounts, setAccounts] = React.useState([]);
  const [balances, setBalances] = React.useState([]);
  const [orders, setOrders] = React.useState([]);
  const [orderBook, setOrderBook] = React.useState(null);
  const [trades, setTrades] = React.useState([]);
  const [marketPrices, setMarketPrices] = React.useState([]);
  const [marketPricesUpdatedAt, setMarketPricesUpdatedAt] = React.useState(null);
  const [marketPricesError, setMarketPricesError] = React.useState('');
  const [status, setStatus] = React.useState({ type: 'idle', text: 'Ready' });
  const [busy, setBusy] = React.useState(false);

  React.useEffect(() => {
    if (auth?.account?.id) {
      selectAccount(String(auth.account.id));
    }
  }, [auth]);

  const request = React.useCallback((path, options = {}) => (
    apiRequest(path, { ...options, token: auth?.token })
  ), [auth?.token]);

  React.useEffect(() => {
    if (!auth?.token) {
      return undefined;
    }

    let cancelled = false;
    let timerId;

    async function refreshMarketPrices() {
      try {
        const data = await request('/market/prices');
        if (!cancelled) {
          setMarketPrices(data);
          setMarketPricesUpdatedAt(new Date().toISOString());
          setMarketPricesError('');
        }
      } catch (error) {
        if (!cancelled) {
          setMarketPricesError(error.message);
        }
      } finally {
        if (!cancelled) {
          timerId = window.setTimeout(refreshMarketPrices, marketPriceRefreshMs);
        }
      }
    }

    refreshMarketPrices();
    return () => {
      cancelled = true;
      window.clearTimeout(timerId);
    };
  }, [auth?.token, request]);

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
    const payload = { username, password };
    const result = await run('Login', () =>
      apiRequest('/auth/login', {
        method: 'POST',
        body: JSON.stringify(payload),
        headers: {}
      })
    );
    if (result) {
      setAuth(result);
      localStorage.setItem('exchange.auth', JSON.stringify(result));
    }
  }

  function logout() {
    setAuth(null);
    setAccounts([]);
    setBalances([]);
    setOrders([]);
    setOrderBook(null);
    setTrades([]);
    setMarketPrices([]);
    setMarketPricesUpdatedAt(null);
    setMarketPricesError('');
    localStorage.removeItem('exchange.auth');
    setStatus({ type: 'idle', text: 'Signed out' });
  }

  function selectAccount(accountId) {
    setSelectedAccountId(accountId);
    setOrder((current) => ({ ...current, accountId }));
  }

  async function loadAccounts() {
    const data = await run('Load accounts', () => request('/accounts'));
    if (data) setAccounts(data);
  }

  async function loadBalances() {
    if (!selectedAccountId) return setStatus({ type: 'error', text: 'Select an account first' });
    const data = await run('Load balances', () => request(`/accounts/${selectedAccountId}/balances`));
    if (data) setBalances(data);
  }

  async function submitDeposit(event) {
    event.preventDefault();
    if (!selectedAccountId) return setStatus({ type: 'error', text: 'Select an account first' });
    const data = await run('Deposit', () =>
      request(`/accounts/${selectedAccountId}/deposit`, {
        method: 'POST',
        body: JSON.stringify({ asset: deposit.asset, amount: Number(deposit.amount) })
      })
    );
    if (data) setBalances(data);
  }

  async function submitOrder(event) {
    event.preventDefault();
    const payload = {
      ...order,
      accountId: Number(order.accountId),
      price: Number(order.price),
      quantity: Number(order.quantity)
    };
    const data = await run('Place order', () =>
      request('/orders', {
        method: 'POST',
        body: JSON.stringify(payload)
      })
    );
    if (data) {
      await Promise.all([loadOrders(), loadOrderBook(), loadTrades(), loadBalances()]);
    }
  }

  async function loadOrders() {
    const query = selectedAccountId ? `?accountId=${selectedAccountId}` : '';
    const data = await run('Load orders', () => request(`/orders${query}`));
    if (data) setOrders(data);
  }

  async function loadOrderBook() {
    const symbol = order.symbol || 'BTC-USDT';
    const data = await run('Load order book', () => request(`/orderbook/${symbol}`));
    if (data) setOrderBook(data);
  }

  async function loadTrades() {
    const symbol = order.symbol || 'BTC-USDT';
    const data = await run('Load trades', () => request(`/trades/${symbol}`));
    if (data) setTrades(data);
  }

  async function loadMarketPrices() {
    try {
      const data = await request('/market/prices');
      setMarketPrices(data);
      setMarketPricesUpdatedAt(new Date().toISOString());
      setMarketPricesError('');
    } catch (error) {
      setMarketPricesError(error.message);
    }
  }

  function refreshAll() {
    return Promise.all([loadAccounts(), loadBalances(), loadOrders(), loadOrderBook(), loadTrades(), loadMarketPrices()]);
  }

  return {
    accounts,
    auth,
    balances,
    busy,
    deposit,
    loadAccounts,
    loadBalances,
    loadOrderBook,
    loadOrders,
    loadTrades,
    loadMarketPrices,
    logout,
    marketPrices,
    marketPricesError,
    marketPricesUpdatedAt,
    order,
    orderBook,
    orders,
    password,
    refreshAll,
    selectedAccountId,
    selectAccount,
    setDeposit,
    setOrder,
    setPassword,
    setUsername,
    status,
    submitAuth,
    submitDeposit,
    submitOrder,
    supportedAssets,
    trades,
    username
  };
}
