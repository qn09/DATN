import React from 'react';
import { apiRequest } from '../api/client.js';

export function useAdminDashboard() {
  const [auth, setAuth] = React.useState(() => {
    const saved = localStorage.getItem('exchange.admin.auth');
    return saved ? JSON.parse(saved) : null;
  });
  const [username, setUsername] = React.useState('admin');
  const [password, setPassword] = React.useState('admin123');
  const [summary, setSummary] = React.useState(null);
  const [accounts, setAccounts] = React.useState([]);
  const [orders, setOrders] = React.useState([]);
  const [trades, setTrades] = React.useState([]);
  const [fiatDeposits, setFiatDeposits] = React.useState([]);
  const [prices, setPrices] = React.useState([]);
  const [status, setStatus] = React.useState({ type: 'idle', text: 'Ready' });
  const [busy, setBusy] = React.useState(false);

  const request = React.useCallback((path, options = {}) => (
    apiRequest(path, { ...options, token: auth?.token })
  ), [auth?.token]);

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

  async function login(event) {
    event.preventDefault();
    const result = await run('Admin login', () =>
      apiRequest('/admin/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password })
      })
    );
    if (!result) return;
    if (result.account.role !== 'ADMIN') {
      setStatus({ type: 'error', text: 'This account is not ADMIN' });
      return;
    }
    setAuth(result);
    localStorage.setItem('exchange.admin.auth', JSON.stringify(result));
  }

  function logout() {
    setAuth(null);
    setSummary(null);
    setAccounts([]);
    setOrders([]);
    setTrades([]);
    setFiatDeposits([]);
    setPrices([]);
    localStorage.removeItem('exchange.admin.auth');
    setStatus({ type: 'idle', text: 'Signed out' });
  }

  async function refreshDashboard() {
    if (!auth) return;
    const data = await run('Refresh admin dashboard', () =>
      Promise.all([
        request('/admin/summary'),
        request('/admin/accounts'),
        request('/admin/orders'),
        request('/admin/trades?limit=50'),
        request('/admin/fiat-deposits?limit=50'),
        request('/admin/market/prices')
      ])
    );
    if (!data) return;
    setSummary(data[0]);
    setAccounts(data[1]);
    setOrders(data[2]);
    setTrades(data[3]);
    setFiatDeposits(data[4]);
    setPrices(data[5]);
  }

  React.useEffect(() => {
    refreshDashboard();
  }, [auth]);

  return {
    accounts,
    auth,
    busy,
    fiatDeposits,
    login,
    logout,
    orders,
    password,
    prices,
    refreshDashboard,
    setPassword,
    setUsername,
    status,
    summary,
    trades,
    username
  };
}
