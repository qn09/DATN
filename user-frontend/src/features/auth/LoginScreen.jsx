import { BarChart3, LockKeyhole, LogIn } from 'lucide-react';

export function LoginScreen({
  busy,
  password,
  status,
  username,
  onPasswordChange,
  onSubmit,
  onUsernameChange
}) {
  return (
    <main className="loginScreen">
      <header className="loginHeader">
        <a className="terminalBrand" href="/" aria-label="Krypto">
          <span className="brandGlyph"><BarChart3 size={20} /></span>
          <strong>KRYPTO</strong>
        </a>
        <span className="networkState"><i /> System operational</span>
      </header>

      <section className="loginShell">
        <div className="loginIntro">
          <span className="loginEyebrow">Spot trading terminal</span>
          <h1>Trade with clarity.</h1>
          <p>Access your markets, portfolio and orders from one secure workspace.</p>
          <div className="loginTape" aria-hidden="true">
            <span>BTC / USDT <b>LIVE</b></span>
            <span>ETH / USDT <b>LIVE</b></span>
            <span>SOL / USDT <b>LIVE</b></span>
          </div>
        </div>

        <div className="loginCard">
          <div className="loginCardHeader">
            <LockKeyhole size={19} />
            <div><h2>Sign in</h2><p>Continue to your trading account</p></div>
          </div>
          <form onSubmit={onSubmit} className="loginForm">
            <label>
              Username
              <input
                autoComplete="username"
                value={username}
                onChange={(event) => onUsernameChange(event.target.value)}
                placeholder="Your username"
              />
            </label>
            <label>
              Password
              <input
                autoComplete="current-password"
                type="password"
                value={password}
                onChange={(event) => onPasswordChange(event.target.value)}
                placeholder="Your password"
              />
            </label>
            {status.type === 'error' && <p className="formError">{status.text}</p>}
            <button className="loginButton" type="submit" disabled={busy}>
              <LogIn size={17} /> {busy ? 'Signing in...' : 'Sign in'}
            </button>
          </form>
        </div>
      </section>
    </main>
  );
}
