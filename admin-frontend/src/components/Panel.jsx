export function Panel({ title, icon, action, children }) {
  return (
    <section className="panel">
      <header className="panelHeader">
        <div>{icon}<h3>{title}</h3></div>
        {action}
      </header>
      {children}
    </section>
  );
}
