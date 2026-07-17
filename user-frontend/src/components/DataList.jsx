export function DataList({ rows, empty }) {
  if (rows.length === 0) return <p className="empty">{empty}</p>;

  return (
    <div className="dataList">
      {rows.map((row) => (
        <div key={row.key} className="dataRow">
          <span>{row.left}</span>
          <strong>{row.right}</strong>
        </div>
      ))}
    </div>
  );
}
