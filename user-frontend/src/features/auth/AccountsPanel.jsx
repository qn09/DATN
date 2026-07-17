import { ListOrdered, RefreshCw } from 'lucide-react';
import { DataList } from '../../components/DataList.jsx';
import { Panel } from '../../components/Panel.jsx';

export function AccountsPanel({ accounts, auth, busy, selectedAccountId, onLoad, onSelectAccount }) {
  return (
    <Panel title="Accounts" icon={<ListOrdered size={18} />}>
      <div className="toolbar">
        <button className="secondaryButton" onClick={onLoad} disabled={!auth || busy}><RefreshCw size={15} /> Load</button>
        <select value={selectedAccountId} onChange={(event) => onSelectAccount(event.target.value)}>
          <option value="">Select account</option>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>#{account.id} {account.username}</option>
          ))}
          {auth && !accounts.some((account) => account.id === auth.account.id) && (
            <option value={auth.account.id}>#{auth.account.id} {auth.account.username}</option>
          )}
        </select>
      </div>
      <DataList
        empty="No accounts loaded"
        rows={accounts.map((account) => ({
          key: account.id,
          left: `#${account.id} ${account.username}`,
          right: account.role
        }))}
      />
    </Panel>
  );
}
