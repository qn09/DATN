import React from 'react';
import { Activity, BarChart3, LogOut, Menu, ShieldCheck, X } from 'lucide-react';

const links = [
  ['Market', '#market'],
  ['Exchange', '#exchange'],
  ['Wallet', '#wallet'],
  ['Orders', '#orders']
];

export function Navbar({ auth, status, onLogout }) {
  const [open, setOpen] = React.useState(false);

  return (
    <header className="siteHeader">
      <a className="siteBrand" href="#top" aria-label="Krypto home">
        <span className="siteBrandMark"><BarChart3 size={22} /></span>
        <span>Krypto</span>
      </a>

      <nav className={open ? 'siteNav open' : 'siteNav'} aria-label="Primary navigation">
        {links.map(([label, href]) => (
          <a key={href} href={href} onClick={() => setOpen(false)}>{label}</a>
        ))}
      </nav>

      <div className="headerActions">
        <div className={`headerStatus ${status.type}`} title={status.text}>
          <Activity size={14} />
          <span>{status.text}</span>
        </div>
        {auth && (
          <div className="headerAccount">
            <ShieldCheck size={16} />
            <span>{auth.account.username}</span>
            <button className="headerIconButton" onClick={onLogout} title="Sign out" aria-label="Sign out">
              <LogOut size={17} />
            </button>
          </div>
        )}
        <button
          className="menuButton"
          onClick={() => setOpen((current) => !current)}
          title="Toggle navigation"
          aria-label="Toggle navigation"
          aria-expanded={open}
        >
          {open ? <X size={21} /> : <Menu size={21} />}
        </button>
      </div>
    </header>
  );
}
