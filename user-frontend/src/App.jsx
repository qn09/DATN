import { LoginScreen } from './features/auth/LoginScreen.jsx';
import { TradingTerminal } from './features/trading/TradingTerminal.jsx';
import { useExchangeConsole } from './hooks/useExchangeConsole.js';

export function App() {
  const exchange = useExchangeConsole();

  if (!exchange.auth) {
    return (
      <LoginScreen
        busy={exchange.busy}
        password={exchange.password}
        status={exchange.status}
        username={exchange.username}
        onPasswordChange={exchange.setPassword}
        onSubmit={exchange.submitAuth}
        onUsernameChange={exchange.setUsername}
      />
    );
  }

  return <TradingTerminal exchange={exchange} />;
}
