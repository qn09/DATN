export function Panel({ title, icon, children }) {
  return (
    <section className="panel">
      <header className="panelHeader">
        <div>{icon}<h3>{title}</h3></div>
      </header>
      {children}
    </section>
  );
}
