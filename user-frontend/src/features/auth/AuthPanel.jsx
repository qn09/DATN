import { KeyRound, LogIn } from 'lucide-react';
import { Panel } from '../../components/Panel.jsx';

export function AuthPanel({
  busy,
  password,
  username,
  onPasswordChange,
  onSubmit,
  onUsernameChange
}) {
  return (
    <Panel title="Authentication" icon={<KeyRound size={18} />}>
      <form onSubmit={onSubmit} className="form">
        <label>
          Username
          <input value={username} onChange={(event) => onUsernameChange(event.target.value)} placeholder="buyer01" />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(event) => onPasswordChange(event.target.value)} placeholder="secret123" />
        </label>
        <button className="primaryButton" type="submit" disabled={busy}>
          <LogIn size={16} />
          Login
        </button>
      </form>
    </Panel>
  );
}
